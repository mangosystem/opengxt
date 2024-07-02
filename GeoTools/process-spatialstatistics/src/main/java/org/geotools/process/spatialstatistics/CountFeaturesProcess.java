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

import org.geotools.api.filter.Filter;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.logging.Logging;

/**
 * Counts the features in the featurecollection
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CountFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CountFeaturesProcess.class);

    public CountFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static Integer process(SimpleFeatureCollection inputFeatures, Filter filter,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CountFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(CountFeaturesProcessFactory.filter.key, filter);

        Process process = new CountFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (Integer) resultMap.get(CountFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return -1;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                CountFeaturesProcessFactory.inputFeatures, null);
        Filter filter = (Filter) Params.getValue(input, CountFeaturesProcessFactory.filter, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        int count = 0;
        if (filter == null) {
            count = inputFeatures.size();
        } else {
            count = inputFeatures.subCollection(filter).size();
        }

        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(CountFeaturesProcessFactory.RESULT.key, Integer.valueOf(count));
        return resultMap;
    }
}
