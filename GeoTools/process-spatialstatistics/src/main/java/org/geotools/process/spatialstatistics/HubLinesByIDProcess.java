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
import org.geotools.process.spatialstatistics.operations.HubLinesByIDOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by id.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HubLinesByIDProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByIDProcess.class);

    private boolean started = false;

    public HubLinesByIDProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection hubFeatures,
            String hubIdField, SimpleFeatureCollection spokeFeatures, String spokeIdField,
            boolean preserveAttributes, boolean useCentroid, double maximumDistance,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HubLinesByIDProcessFactory.hubFeatures.key, hubFeatures);
        map.put(HubLinesByIDProcessFactory.hubIdField.key, hubIdField);
        map.put(HubLinesByIDProcessFactory.spokeFeatures.key, spokeFeatures);
        map.put(HubLinesByIDProcessFactory.spokeIdField.key, spokeIdField);
        map.put(HubLinesByIDProcessFactory.preserveAttributes.key, preserveAttributes);
        map.put(HubLinesByIDProcessFactory.useCentroid.key, useCentroid);
        map.put(HubLinesByIDProcessFactory.maximumDistance.key, maximumDistance);

        Process process = new HubLinesByIDProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(HubLinesByIDProcessFactory.RESULT.key);
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

            SimpleFeatureCollection hubFeatures = (SimpleFeatureCollection) Params.getValue(input,
                    HubLinesByIDProcessFactory.hubFeatures, null);
            String hubIdField = (String) Params.getValue(input,
                    HubLinesByIDProcessFactory.hubIdField, null);

            SimpleFeatureCollection spokeFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, HubLinesByIDProcessFactory.spokeFeatures, null);
            String spokeIdField = (String) Params.getValue(input,
                    HubLinesByIDProcessFactory.spokeIdField, null);
            if (hubFeatures == null || hubIdField == null || spokeFeatures == null
                    || spokeIdField == null) {
                throw new NullPointerException(
                        "hubFeatures, hubIdField, spokeFeatures, spokeIdField parameters required");
            }

            Boolean preserveAttributes = (Boolean) Params.getValue(input,
                    HubLinesByIDProcessFactory.preserveAttributes,
                    HubLinesByIDProcessFactory.preserveAttributes.sample);
            Boolean useCentroid = (Boolean) Params.getValue(input,
                    HubLinesByIDProcessFactory.useCentroid,
                    HubLinesByIDProcessFactory.useCentroid.sample);
            Double maximumDistance = (Double) Params.getValue(input,
                    HubLinesByIDProcessFactory.maximumDistance,
                    HubLinesByIDProcessFactory.maximumDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            HubLinesByIDOperation operation = new HubLinesByIDOperation();
            SimpleFeatureCollection resultFc = operation.execute(hubFeatures, hubIdField,
                    spokeFeatures, spokeIdField, useCentroid, preserveAttributes, maximumDistance);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(HubLinesByIDProcessFactory.RESULT.key, resultFc);
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
