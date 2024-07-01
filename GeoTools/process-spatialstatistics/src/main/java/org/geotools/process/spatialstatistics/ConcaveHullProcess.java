/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.ConcaveHullOperation;
import org.geotools.util.logging.Logging;

/**
 * Creates a concave hull using the alpha shapes algorithm.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ConcaveHullProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ConcaveHullProcess.class);

    public ConcaveHullProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression group, Double alpha, Boolean removeHoles, Boolean splitMultipart,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ConcaveHullProcessFactory.features.key, features);
        map.put(ConcaveHullProcessFactory.group.key, group);
        map.put(ConcaveHullProcessFactory.alpha.key, alpha);
        map.put(ConcaveHullProcessFactory.removeHoles.key, removeHoles);
        map.put(ConcaveHullProcessFactory.splitMultipart.key, splitMultipart);

        Process process = new ConcaveHullProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(ConcaveHullProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                ConcaveHullProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Expression group = (Expression) Params.getValue(input, ConcaveHullProcessFactory.group,
                null);

        Double alpha = (Double) Params.getValue(input, ConcaveHullProcessFactory.alpha,
                ConcaveHullProcessFactory.alpha.sample);

        Boolean removeHoles = (Boolean) Params.getValue(input,
                ConcaveHullProcessFactory.removeHoles,
                ConcaveHullProcessFactory.removeHoles.sample);

        Boolean splitMultipart = (Boolean) Params.getValue(input,
                ConcaveHullProcessFactory.splitMultipart,
                ConcaveHullProcessFactory.splitMultipart.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            ConcaveHullOperation process = new ConcaveHullOperation();
            resultFc = process.execute(features, group, alpha, removeHoles, splitMultipart);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ConcaveHullProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
