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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.operations.NearestNeighborCountOperation;
import org.geotools.util.logging.Logging;

/**
 * Calculates count between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NearestNeighborCountProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(NearestNeighborCountProcess.class);

    public NearestNeighborCountProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String countField, SimpleFeatureCollection nearFeatures, double searchRadius,
            ProgressListener monitor) {
        return NearestNeighborCountProcess.process(inputFeatures, countField, nearFeatures,
                searchRadius, DistanceUnit.Default, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String countField, SimpleFeatureCollection nearFeatures, double searchRadius,
            DistanceUnit radiusUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NearestNeighborCountProcessFactory.inputFeatures.key, inputFeatures);
        map.put(NearestNeighborCountProcessFactory.countField.key, countField);
        map.put(NearestNeighborCountProcessFactory.nearFeatures.key, nearFeatures);
        map.put(NearestNeighborCountProcessFactory.searchRadius.key, searchRadius);
        map.put(NearestNeighborCountProcessFactory.radiusUnit.key, radiusUnit);

        Process process = new NearestNeighborCountProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(NearestNeighborCountProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                NearestNeighborCountProcessFactory.inputFeatures, null);

        SimpleFeatureCollection nearFeatures = (SimpleFeatureCollection) Params.getValue(input,
                NearestNeighborCountProcessFactory.nearFeatures, null);
        if (nearFeatures == null || inputFeatures == null) {
            throw new NullPointerException("nearFeatures, inputFeatures parameters required");
        }

        String countField = (String) Params.getValue(input,
                NearestNeighborCountProcessFactory.countField,
                NearestNeighborCountProcessFactory.countField.sample);

        Double searchRadius = (Double) Params.getValue(input,
                NearestNeighborCountProcessFactory.searchRadius,
                NearestNeighborCountProcessFactory.searchRadius.sample);

        DistanceUnit radiusUnit = (DistanceUnit) Params.getValue(input,
                NearestNeighborCountProcessFactory.radiusUnit,
                NearestNeighborCountProcessFactory.radiusUnit.sample);

        if (Double.isNaN(searchRadius) || Double.isInfinite(searchRadius) || searchRadius <= 0) {
            throw new ProcessException("Search radius must be greater than 0!");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            NearestNeighborCountOperation operation = new NearestNeighborCountOperation();
            resultFc = operation.execute(inputFeatures, countField, nearFeatures, searchRadius,
                    radiusUnit);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(NearestNeighborCountProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
