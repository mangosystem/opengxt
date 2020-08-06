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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.CircularBinningOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Performs rectangular binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircularBinningProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CircularBinningProcess.class);

    public CircularBinningProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression weight, ReferencedEnvelope bbox, Double radius, Boolean validGrid,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CircularBinningProcessFactory.features.key, features);
        map.put(CircularBinningProcessFactory.weight.key, weight);
        map.put(CircularBinningProcessFactory.bbox.key, bbox);
        map.put(CircularBinningProcessFactory.radius.key, radius);
        map.put(CircularBinningProcessFactory.validGrid.key, validGrid);

        Process process = new CircularBinningProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(CircularBinningProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                CircularBinningProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Expression weight = (Expression) Params.getValue(input,
                CircularBinningProcessFactory.weight, null);
        ReferencedEnvelope bbox = (ReferencedEnvelope) Params.getValue(input,
                CircularBinningProcessFactory.bbox, null);
        Double radius = (Double) Params.getValue(input, CircularBinningProcessFactory.radius, null);
        Boolean validGrid = (Boolean) Params.getValue(input,
                CircularBinningProcessFactory.validGrid,
                CircularBinningProcessFactory.validGrid.sample);

        // start process
        if (bbox == null || bbox.isEmpty()) {
            bbox = features.getBounds();
        }

        if (radius == null || radius <= 0) {
            radius = Math.min(bbox.getWidth(), bbox.getHeight()) / 40.0d;
            LOGGER.log(Level.WARNING, "The default circle radius is " + radius);
        }

        SimpleFeatureCollection resultFc = null;
        try {
            CircularBinningOperation process = new CircularBinningOperation();
            process.setOnlyValidGrid(validGrid);
            resultFc = process.execute(features, weight, bbox, radius);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(CircularBinningProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}