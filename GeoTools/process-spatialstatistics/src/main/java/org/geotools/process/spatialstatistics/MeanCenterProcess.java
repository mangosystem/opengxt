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
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Identifies the geographic center (or the center of concentration) for a set of features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MeanCenterProcess extends AbstractProcess {
    protected static final Logger LOGGER = Logging.getLogger(MeanCenterProcess.class);

    private boolean started = false;

    public MeanCenterProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String weightField, String caseField, String dimensionField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MeanCenterProcessFactory.inputFeatures.key, inputFeatures);
        map.put(MeanCenterProcessFactory.weightField.key, weightField);
        map.put(MeanCenterProcessFactory.caseField.key, caseField);
        map.put(MeanCenterProcessFactory.dimensionField.key, dimensionField);

        Process process = new MeanCenterProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(MeanCenterProcessFactory.RESULT.key);
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
                    input, MeanCenterProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameters required");
            }
            String weightField = (String) Params.getValue(input,
                    MeanCenterProcessFactory.weightField, null);
            String caseField = (String) Params.getValue(input, MeanCenterProcessFactory.caseField,
                    null);
            String dimensionField = (String) Params.getValue(input,
                    MeanCenterProcessFactory.dimensionField, null);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            // TODO: implement
            SimpleFeatureCollection resultFc = inputFeatures;
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(MeanCenterProcessFactory.RESULT.key, resultFc);
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
