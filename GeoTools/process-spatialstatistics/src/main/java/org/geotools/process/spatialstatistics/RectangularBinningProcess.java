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
import org.geotools.process.spatialstatistics.pattern.RectangularBinningOperation;
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
public class RectangularBinningProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RectangularBinningProcess.class);

    public RectangularBinningProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression weight, ReferencedEnvelope bbox, Double width, Double height,
            Boolean validGrid, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RectangularBinningProcessFactory.features.key, features);
        map.put(RectangularBinningProcessFactory.weight.key, weight);
        map.put(RectangularBinningProcessFactory.bbox.key, bbox);
        map.put(RectangularBinningProcessFactory.width.key, width);
        map.put(RectangularBinningProcessFactory.height.key, height);
        map.put(RectangularBinningProcessFactory.validGrid.key, validGrid);

        Process process = new RectangularBinningProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RectangularBinningProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                RectangularBinningProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Expression weight = (Expression) Params.getValue(input,
                RectangularBinningProcessFactory.weight, null);
        ReferencedEnvelope bbox = (ReferencedEnvelope) Params.getValue(input,
                RectangularBinningProcessFactory.bbox, null);
        Double width = (Double) Params
                .getValue(input, RectangularBinningProcessFactory.width, null);
        Double height = (Double) Params.getValue(input, RectangularBinningProcessFactory.height,
                null);
        Boolean validGrid = (Boolean) Params.getValue(input,
                RectangularBinningProcessFactory.validGrid,
                RectangularBinningProcessFactory.validGrid.sample);

        // start process
        if (bbox == null || bbox.isEmpty()) {
            bbox = features.getBounds();
        }

        if (width == null || width <= 0 || height == null || height <= 0) {
            width = Math.min(bbox.getWidth(), bbox.getHeight()) / 250.0d;
            height = width;
            LOGGER.log(Level.WARNING, "The default width / height is " + width);
        }

        SimpleFeatureCollection resultFc = null;
        try {
            RectangularBinningOperation process = new RectangularBinningOperation();
            process.setOnlyValidGrid(validGrid);
            resultFc = process.execute(features, weight, bbox, width, height);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RectangularBinningProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}