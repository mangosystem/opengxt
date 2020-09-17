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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.SmoothFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Smooths the geometries in a line or polygon layer.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SmoothProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SmoothProcess.class);

    public SmoothProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features, Double fit,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SmoothProcessFactory.features.key, features);
        map.put(SmoothProcessFactory.fit.key, fit);

        Process process = new SmoothProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(SmoothProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                SmoothProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Double fit = (Double) Params.getValue(input, SmoothProcessFactory.fit,
                SmoothProcessFactory.fit.sample);

        // start process
        SimpleFeatureCollection resultFc = new SmoothFeatureCollection(features, fit);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SmoothProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
