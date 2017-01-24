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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.MergeFeaturesOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Combines multiple input features of the same data type into a single, new output features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MergeFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(MergeFeaturesProcess.class);

    public MergeFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(Collection<SimpleFeatureCollection> features,
            Filter filter, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MergeFeaturesProcessFactory.features.key, features);

        Process process = new MergeFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(MergeFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        @SuppressWarnings("unchecked")
        Collection<SimpleFeatureCollection> features = (Collection<SimpleFeatureCollection>) Params
                .getValue(input, MergeFeaturesProcessFactory.features, null);
        if (features == null || features.size() == 0) {
            throw new NullPointerException("features parameter required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            MergeFeaturesOperation operation = new MergeFeaturesOperation();
            resultFc = operation.execute(features, null);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(MergeFeaturesProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
