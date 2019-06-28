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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.operations.NearOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates distance and additional proximity information between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NearProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(NearProcess.class);

    public NearProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField, double maximumDistance,
            DistanceUnit distanceUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NearProcessFactory.inputFeatures.key, inputFeatures);
        map.put(NearProcessFactory.nearFeatures.key, nearFeatures);
        map.put(NearProcessFactory.nearIdField.key, nearIdField);
        map.put(NearProcessFactory.maximumDistance.key, maximumDistance);
        map.put(NearProcessFactory.distanceUnit.key, distanceUnit);

        Process process = new NearProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(NearProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                NearProcessFactory.inputFeatures, null);

        SimpleFeatureCollection nearFeatures = (SimpleFeatureCollection) Params.getValue(input,
                NearProcessFactory.nearFeatures, null);
        String nearIdField = (String) Params.getValue(input, NearProcessFactory.nearIdField, null);
        if (nearFeatures == null || inputFeatures == null) {
            throw new NullPointerException("nearFeatures, inputFeatures parameters required");
        }

        Double maximumDistance = (Double) Params.getValue(input,
                NearProcessFactory.maximumDistance, NearProcessFactory.maximumDistance.sample);

        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                NearProcessFactory.distanceUnit, NearProcessFactory.distanceUnit.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            NearOperation operation = new NearOperation();
            resultFc = operation.execute(inputFeatures, nearFeatures, nearIdField, maximumDistance,
                    distanceUnit);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(NearProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
