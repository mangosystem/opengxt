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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.autocorrelation.LocalRogersonROperation;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;

/**
 * Detect spatial clusters based on feature locations and attribute values using the Global Rogerson's R statistic.
 * 
 * @reference
 * 
 *            Peter A. Rogerson (1999) "The Detection of Clusters Using a Spatial Version of the Chi-Square Goodness-of-Fit Statistic", Geographical
 *            Analysis, 31:130–147<br>
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalRogersonRProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalRogersonRProcess.class);

    public LocalRogersonRProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String xField, String yField, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance, Double kappa, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalRogersonRProcessFactory.inputFeatures.key, inputFeatures);
        map.put(LocalRogersonRProcessFactory.xField.key, xField);
        map.put(LocalRogersonRProcessFactory.yField.key, yField);
        map.put(LocalRogersonRProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalRogersonRProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalRogersonRProcessFactory.standardization.key, standardization);
        map.put(LocalRogersonRProcessFactory.searchDistance.key, searchDistance);
        map.put(LocalRogersonRProcessFactory.kappa.key, kappa);

        Process process = new LocalRogersonRProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(LocalRogersonRProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                LocalRogersonRProcessFactory.inputFeatures, null);
        String xField = (String) Params.getValue(input, LocalRogersonRProcessFactory.xField, null);
        String yField = (String) Params.getValue(input, LocalRogersonRProcessFactory.yField, null);
        if (inputFeatures == null || xField == null || yField == null) {
            throw new NullPointerException("inputFeatures, xField, yField parameters required");
        }

        xField = FeatureTypes.validateProperty(inputFeatures.getSchema(), xField);
        if (inputFeatures.getSchema().indexOf(xField) == -1) {
            throw new NullPointerException(xField + " field does not exist!");
        }

        yField = FeatureTypes.validateProperty(inputFeatures.getSchema(), yField);
        if (inputFeatures.getSchema().indexOf(yField) == -1) {
            throw new NullPointerException(yField + " field does not exist!");
        }

        SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                LocalRogersonRProcessFactory.spatialConcept,
                LocalRogersonRProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                LocalRogersonRProcessFactory.distanceMethod,
                LocalRogersonRProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                LocalRogersonRProcessFactory.standardization,
                LocalRogersonRProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                LocalRogersonRProcessFactory.searchDistance,
                LocalRogersonRProcessFactory.searchDistance.sample);

        Double kappa = (Double) Params.getValue(input, LocalRogersonRProcessFactory.kappa,
                LocalRogersonRProcessFactory.kappa.sample);

        // start process
        SimpleFeatureCollection resultFc = null;

        LocalRogersonROperation process = new LocalRogersonROperation();
        process.setSpatialConceptType(spatialConcept);
        process.setDistanceType(distanceMethod);
        process.setStandardizationType(standardization);
        process.setKappa(kappa);

        // searchDistance
        if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
            process.setDistanceBand(searchDistance);
        }

        try {
            resultFc = process.execute(inputFeatures, xField, yField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(LocalRogersonRProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
