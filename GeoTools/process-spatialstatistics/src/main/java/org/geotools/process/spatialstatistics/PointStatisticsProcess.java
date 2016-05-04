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
import org.geotools.process.spatialstatistics.operations.PointStatisticsOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Perform point in polygon analysis
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointStatisticsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PointStatisticsProcess.class);

    private boolean started = false;

    public PointStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection polygonFeatures,
            SimpleFeatureCollection pointFeatures, String countField, String statisticsFields,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PointStatisticsProcessFactory.polygonFeatures.key, polygonFeatures);
        map.put(PointStatisticsProcessFactory.pointFeatures.key, pointFeatures);
        map.put(PointStatisticsProcessFactory.countField.key, countField);
        map.put(PointStatisticsProcessFactory.statisticsFields.key, statisticsFields);

        Process process = new PointStatisticsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(PointStatisticsProcessFactory.RESULT.key);
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

            SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, PointStatisticsProcessFactory.polygonFeatures, null);
            SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, PointStatisticsProcessFactory.pointFeatures, null);
            String countField = (String) Params.getValue(input,
                    PointStatisticsProcessFactory.countField,
                    PointStatisticsProcessFactory.countField.sample);
            String statisticsFields = (String) Params.getValue(input,
                    PointStatisticsProcessFactory.statisticsFields,
                    PointStatisticsProcessFactory.statisticsFields.sample);
            if (polygonFeatures == null || pointFeatures == null) {
                throw new NullPointerException(
                        "polygonFeatures and pointFeatures parameters required");
            }

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            PointStatisticsOperation operation = new PointStatisticsOperation();
            operation.setBufferDistance(0.0);
            SimpleFeatureCollection resultFc = operation.execute(polygonFeatures, countField,
                    statisticsFields, pointFeatures);

            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(PointStatisticsProcessFactory.RESULT.key, resultFc);
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
