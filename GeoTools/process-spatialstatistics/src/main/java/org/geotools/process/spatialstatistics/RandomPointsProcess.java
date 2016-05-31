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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.RandomPointsOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Generate random points in as extent or polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RandomPointsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RandomPointsProcess.class);

    private boolean started = false;

    public RandomPointsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(Integer pointCount, ReferencedEnvelope extent,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RandomPointsProcessFactory.pointCount.key, pointCount);
        map.put(RandomPointsProcessFactory.extent.key, extent);

        Process process = new RandomPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RandomPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    public static SimpleFeatureCollection process(Integer pointCount,
            SimpleFeatureCollection inputFeatures, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RandomPointsProcessFactory.pointCount.key, pointCount);
        map.put(RandomPointsProcessFactory.polygonFeatures.key, inputFeatures);

        Process process = new RandomPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RandomPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            int pointCount = (Integer) Params.getValue(input,
                    RandomPointsProcessFactory.pointCount,
                    RandomPointsProcessFactory.pointCount.sample);
            if (pointCount < 1) {
                throw new NullPointerException("Point count must be greater than 1");
            }

            SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, RandomPointsProcessFactory.polygonFeatures, null);
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    RandomPointsProcessFactory.extent, null);
            if (polygonFeatures == null && extent == null) {
                throw new NullPointerException("extent or polygonFeatures parameters required");
            }

            // start process
            RandomPointsOperation operator = new RandomPointsOperation();
            SimpleFeatureCollection randomPoints = null;

            if (polygonFeatures == null) {
                randomPoints = operator.execute(extent, pointCount);
            } else {
                randomPoints = operator.execute(polygonFeatures, pointCount);
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AreaProcessFactory.RESULT.key, randomPoints);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
