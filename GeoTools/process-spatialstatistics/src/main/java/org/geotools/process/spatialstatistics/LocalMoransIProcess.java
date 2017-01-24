/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.autocorrelation.LocalMoranIStatisticOperation;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Given a set of weighted features, identifies statistically significant hot spots, cold spots, and spatial outliers using the Anselin Local Moran's
 * I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalMoransIProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalMoransIProcess.class);

    public LocalMoransIProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, Boolean selfNeighbors,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalMoransIProcessFactory.inputFeatures.key, inputFeatures);
        map.put(LocalMoransIProcessFactory.inputField.key, inputField);
        map.put(LocalMoransIProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalMoransIProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalMoransIProcessFactory.standardization.key, standardization);
        map.put(LocalMoransIProcessFactory.searchDistance.key, searchDistance);
        map.put(LocalMoransIProcessFactory.selfNeighbors.key, selfNeighbors);

        Process process = new LocalMoransIProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(LocalMoransIProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                LocalMoransIProcessFactory.inputFeatures, null);
        String inputField = (String) Params.getValue(input, LocalMoransIProcessFactory.inputField,
                null);
        if (inputFeatures == null || inputField == null) {
            throw new NullPointerException("inputFeatures and inputField parameters required");
        }

        inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
        if (inputFeatures.getSchema().indexOf(inputField) == -1) {
            throw new NullPointerException(inputField + " field does not exist!");
        }

        SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                LocalMoransIProcessFactory.spatialConcept,
                LocalMoransIProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                LocalMoransIProcessFactory.distanceMethod,
                LocalMoransIProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                LocalMoransIProcessFactory.standardization,
                LocalMoransIProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                LocalMoransIProcessFactory.searchDistance,
                LocalMoransIProcessFactory.searchDistance.sample);

        Boolean selfNeighbors = (Boolean) Params.getValue(input,
                LocalMoransIProcessFactory.selfNeighbors,
                LocalMoransIProcessFactory.selfNeighbors.sample);

        // start process
        SimpleFeatureCollection resultFc = null;

        LocalMoranIStatisticOperation process = new LocalMoranIStatisticOperation();
        process.setSpatialConceptType(spatialConcept);
        process.setDistanceType(distanceMethod);
        process.setStandardizationType(standardization);
        process.setSelfNeighbors(selfNeighbors);

        // searchDistance
        if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
            process.setDistanceBand(searchDistance);
        }

        try {
            resultFc = process.execute(inputFeatures, inputField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(LocalMoransIProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
