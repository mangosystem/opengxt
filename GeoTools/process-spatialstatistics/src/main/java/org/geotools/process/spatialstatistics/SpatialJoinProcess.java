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
import org.geotools.process.spatialstatistics.enumeration.SpatialJoinType;
import org.geotools.process.spatialstatistics.operations.SpatialJoinOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Perform spatial join
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialJoinProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SpatialJoinProcess.class);

    public SpatialJoinProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection joinFeatures, SpatialJoinType joinType, Double searchRadius,
            ProgressListener monitor) {
        return SpatialJoinProcess.process(inputFeatures, joinFeatures, joinType, searchRadius,
                DistanceUnit.Default, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection joinFeatures, SpatialJoinType joinType, Double searchRadius,
            DistanceUnit radiusUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SpatialJoinProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SpatialJoinProcessFactory.joinFeatures.key, joinFeatures);
        map.put(SpatialJoinProcessFactory.joinType.key, joinType);
        map.put(SpatialJoinProcessFactory.searchRadius.key, searchRadius);
        map.put(SpatialJoinProcessFactory.radiusUnit.key, radiusUnit);

        Process process = new SpatialJoinProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(SpatialJoinProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SpatialJoinProcessFactory.inputFeatures, null);
        SimpleFeatureCollection joinFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SpatialJoinProcessFactory.joinFeatures, null);
        if (inputFeatures == null || joinFeatures == null) {
            throw new NullPointerException("inputFeatures and joinFeatures parameters required");
        }
        SpatialJoinType joinType = (SpatialJoinType) Params.getValue(input,
                SpatialJoinProcessFactory.joinType, SpatialJoinProcessFactory.joinType.sample);
        Double searchRadius = (Double) Params.getValue(input,
                SpatialJoinProcessFactory.searchRadius,
                SpatialJoinProcessFactory.searchRadius.sample);
        DistanceUnit radiusUnit = (DistanceUnit) Params.getValue(input,
                SpatialJoinProcessFactory.radiusUnit, SpatialJoinProcessFactory.radiusUnit.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            SpatialJoinOperation operation = new SpatialJoinOperation();
            resultFc = operation.execute(inputFeatures, joinFeatures, joinType, searchRadius,
                    radiusUnit);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SpatialJoinProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
