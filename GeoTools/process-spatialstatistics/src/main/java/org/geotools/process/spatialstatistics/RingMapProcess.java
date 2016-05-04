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
import org.geotools.process.spatialstatistics.operations.RingMapsOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a ring map from features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RingMapProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RingMapProcess.class);

    private boolean started = false;

    public RingMapProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String fields, String targetField, Integer ringGap, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RingMapProcessFactory.inputFeatures.key, inputFeatures);
        map.put(RingMapProcessFactory.fields.key, fields);
        map.put(RingMapProcessFactory.targetField.key, targetField);
        map.put(RingMapProcessFactory.ringGap.key, ringGap);

        Process process = new RingMapProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RingMapProcessFactory.ringmap.key);
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
                    input, RingMapProcessFactory.inputFeatures, null);
            String fields = (String) Params.getValue(input, RingMapProcessFactory.fields, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures, fields parameters required");
            }
            String targetField = (String) Params.getValue(input, RingMapProcessFactory.targetField,
                    RingMapProcessFactory.targetField.sample);
            Integer ringGap = (Integer) Params.getValue(input, RingMapProcessFactory.ringGap,
                    RingMapProcessFactory.ringGap.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            SimpleFeatureCollection ringFc = null;
            SimpleFeatureCollection anchorFc = null;
            RingMapsOperation operation = new RingMapsOperation();
            if (operation.execute(inputFeatures, fields, targetField, ringGap)) {
                ringFc = operation.getRingFc();
                anchorFc = operation.getAnchorFc();
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(RingMapProcessFactory.ringmap.key, ringFc);
            resultMap.put(RingMapProcessFactory.anchor.key, anchorFc);
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
