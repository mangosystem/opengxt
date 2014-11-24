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
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.RandomPointsOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates area values for each feature in a polygon features.
 * 
 * @author Minpa Lee, MangoSystem RandomShapeFactory
 * 
 * @source $URL$
 */
public class RandomPointsProcess extends AbstractProcess {
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

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

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

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            RandomPointsOperation operator = null;
            if (polygonFeatures == null) {
                operator = new RandomPointsOperation(extent);
            } else {
                operator = new RandomPointsOperation(polygonFeatures);
            }
            SimpleFeatureCollection randomPoints = operator.execute(pointCount);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AreaProcessFactory.RESULT.key, randomPoints);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }
}
