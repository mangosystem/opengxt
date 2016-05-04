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
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Queries features using an optional filter and an optional list of attributes to include.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SelectFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SelectFeaturesProcess.class);

    private boolean started = false;

    public SelectFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Filter filter, String attributes, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SelectFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SelectFeaturesProcessFactory.filter.key, filter);
        map.put(SelectFeaturesProcessFactory.attributes.key, attributes);

        Process process = new SelectFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(SelectFeaturesProcessFactory.RESULT.key);
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
                    input, SelectFeaturesProcessFactory.inputFeatures, null);
            Filter filter = (Filter) Params.getValue(input, SelectFeaturesProcessFactory.filter,
                    null);
            String attributes = (String) Params.getValue(input,
                    SelectFeaturesProcessFactory.attributes, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameter required");
            }

            monitor.setTask(Text.text("Processing " + this.getClass().getSimpleName()));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            // apply filtering if necessary
            SimpleFeatureCollection resultFc = inputFeatures;
            if (filter != null && !filter.equals(Filter.INCLUDE)) {
                resultFc = new FilteringSimpleFeatureCollection(inputFeatures, filter);
            }

            // apply retyping if necessary
            if (attributes != null && attributes.length() > 0) {
                String[] names = attributes.split(",");
                for (int idx = 0; idx < names.length; idx++) {
                    names[idx] = names[idx].trim();
                }

                SimpleFeatureType ft = SimpleFeatureTypeBuilder.retype(resultFc.getSchema(), names);
                if (!(ft.equals(resultFc.getSchema()))) {
                    resultFc = new ReTypingFeatureCollection(resultFc, ft);
                }
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(SelectFeaturesProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            throw new ProcessException(eek);
        } finally {
            monitor.dispose();
            started = false;
        }
    }
}
