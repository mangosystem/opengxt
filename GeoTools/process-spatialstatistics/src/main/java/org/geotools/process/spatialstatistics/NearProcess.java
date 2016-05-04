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
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.NearOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
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

    private boolean started = false;

    public NearProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField, double maximumDistance,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NearProcessFactory.inputFeatures.key, inputFeatures);
        map.put(NearProcessFactory.nearFeatures.key, nearFeatures);
        map.put(NearProcessFactory.nearIdField.key, nearIdField);
        map.put(NearProcessFactory.maximumDistance.key, maximumDistance);

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
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, NearProcessFactory.inputFeatures, null);

            SimpleFeatureCollection nearFeatures = (SimpleFeatureCollection) Params.getValue(input,
                    NearProcessFactory.nearFeatures, null);
            String nearIdField = (String) Params.getValue(input, NearProcessFactory.nearIdField,
                    null);
            if (nearFeatures == null || inputFeatures == null) {
                throw new NullPointerException("nearFeatures, inputFeatures parameters required");
            }

            Double maximumDistance = (Double) Params.getValue(input,
                    NearProcessFactory.maximumDistance, NearProcessFactory.maximumDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            NearOperation operation = new NearOperation();
            SimpleFeatureCollection resultFc = operation.execute(inputFeatures, nearFeatures,
                    nearIdField, maximumDistance);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(NearProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
            started = false;
        }
    }

}
