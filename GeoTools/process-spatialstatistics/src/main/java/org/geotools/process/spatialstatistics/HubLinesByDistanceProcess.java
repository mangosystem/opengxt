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
import org.geotools.process.spatialstatistics.operations.HubLinesByDistanceOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by nearest distance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HubLinesByDistanceProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByDistanceProcess.class);

    private boolean started = false;

    public HubLinesByDistanceProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection spokeFeatures,
            SimpleFeatureCollection hubFeatures, String hubIdField, boolean preserveAttributes,
            boolean useCentroid, double maximumDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HubLinesByDistanceProcessFactory.spokeFeatures.key, spokeFeatures);
        map.put(HubLinesByDistanceProcessFactory.hubFeatures.key, hubFeatures);
        map.put(HubLinesByDistanceProcessFactory.hubIdField.key, hubIdField);
        map.put(HubLinesByDistanceProcessFactory.preserveAttributes.key, preserveAttributes);
        map.put(HubLinesByDistanceProcessFactory.useCentroid.key, useCentroid);
        map.put(HubLinesByDistanceProcessFactory.maximumDistance.key, maximumDistance);

        Process process = new HubLinesByDistanceProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(HubLinesByDistanceProcessFactory.RESULT.key);
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

            SimpleFeatureCollection spokeFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, HubLinesByDistanceProcessFactory.spokeFeatures, null);

            SimpleFeatureCollection hubFeatures = (SimpleFeatureCollection) Params.getValue(input,
                    HubLinesByDistanceProcessFactory.hubFeatures, null);
            String hubIdField = (String) Params.getValue(input,
                    HubLinesByDistanceProcessFactory.hubIdField, null);
            if (hubFeatures == null || spokeFeatures == null) {
                throw new NullPointerException(
                        "hubFeatures, spokeFeatures parameters required");
            }

            Boolean preserveAttributes = (Boolean) Params.getValue(input,
                    HubLinesByDistanceProcessFactory.preserveAttributes,
                    HubLinesByDistanceProcessFactory.preserveAttributes.sample);
            Boolean useCentroid = (Boolean) Params.getValue(input,
                    HubLinesByDistanceProcessFactory.useCentroid,
                    HubLinesByDistanceProcessFactory.useCentroid.sample);
            Double maximumDistance = (Double) Params.getValue(input,
                    HubLinesByDistanceProcessFactory.maximumDistance,
                    HubLinesByDistanceProcessFactory.maximumDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            HubLinesByDistanceOperation operation = new HubLinesByDistanceOperation();
            SimpleFeatureCollection resultFc = operation.execute(spokeFeatures, hubFeatures,
                    hubIdField, useCentroid, preserveAttributes, maximumDistance);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(HubLinesByDistanceProcessFactory.RESULT.key, resultFc);
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
