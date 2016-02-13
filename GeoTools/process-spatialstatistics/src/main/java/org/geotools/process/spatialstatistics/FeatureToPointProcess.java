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
import org.geotools.process.spatialstatistics.transformation.ExplodeFeatureCollection;
import org.geotools.process.spatialstatistics.transformation.ToPointFeatureCollection;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a point features generated from the representative locations of input features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureToPointProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FeatureToPointProcess.class);

    private boolean started = false;

    public FeatureToPointProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Boolean inside, Boolean singlePart, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FeatureToPointProcessFactory.inputFeatures.key, inputFeatures);
        map.put(FeatureToPointProcessFactory.inside.key, inside);
        map.put(FeatureToPointProcessFactory.singlePart.key, singlePart);

        Process process = new FeatureToPointProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(FeatureToPointProcessFactory.RESULT.key);
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
                    input, FeatureToPointProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameter required");
            }

            Boolean inside = (Boolean) Params.getValue(input, FeatureToPointProcessFactory.inside,
                    FeatureToPointProcessFactory.inside.sample);
            Boolean singlePart = (Boolean) Params.getValue(input,
                    FeatureToPointProcessFactory.singlePart,
                    FeatureToPointProcessFactory.singlePart.sample);

            monitor.setTask(Text.text("Processing " + this.getClass().getSimpleName()));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            SimpleFeatureCollection resultFc = null;
            if (singlePart) {
                resultFc = new ToPointFeatureCollection(
                        new ExplodeFeatureCollection(inputFeatures), inside);
            } else {
                resultFc = new ToPointFeatureCollection(inputFeatures, inside);
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(FeatureToPointProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            throw new ProcessException(eek);
        } finally {
            monitor.dispose();
        }
    }
}
