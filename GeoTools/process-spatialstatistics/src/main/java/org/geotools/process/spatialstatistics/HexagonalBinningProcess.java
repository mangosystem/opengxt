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

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.HexagonalBinningOperation;
import org.geotools.util.logging.Logging;

/**
 * Performs hexagonal binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HexagonalBinningProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(HexagonalBinningProcess.class);

    public HexagonalBinningProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression weight, ReferencedEnvelope bbox, Double size, Boolean validGrid,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HexagonalBinningProcessFactory.features.key, features);
        map.put(HexagonalBinningProcessFactory.weight.key, weight);
        map.put(HexagonalBinningProcessFactory.bbox.key, bbox);
        map.put(HexagonalBinningProcessFactory.size.key, size);
        map.put(HexagonalBinningProcessFactory.validGrid.key, validGrid);

        Process process = new HexagonalBinningProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(HexagonalBinningProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                HexagonalBinningProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Expression weight = (Expression) Params.getValue(input,
                HexagonalBinningProcessFactory.weight, null);
        ReferencedEnvelope bbox = (ReferencedEnvelope) Params.getValue(input,
                HexagonalBinningProcessFactory.bbox, null);
        Double size = (Double) Params.getValue(input, HexagonalBinningProcessFactory.size, null);
        Boolean validGrid = (Boolean) Params.getValue(input,
                HexagonalBinningProcessFactory.validGrid,
                HexagonalBinningProcessFactory.validGrid.sample);

        // start process
        if (bbox == null || bbox.isEmpty()) {
            bbox = features.getBounds();
        }

        if (size == null || size <= 0) {
            size = Math.min(bbox.getWidth(), bbox.getHeight()) / 40.0d;
            LOGGER.log(Level.WARNING, "The default grid size is " + size);
        }

        SimpleFeatureCollection resultFc = null;
        try {
            HexagonalBinningOperation process = new HexagonalBinningOperation();
            process.setOnlyValidGrid(validGrid);
            resultFc = process.execute(features, weight, bbox, size);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(HexagonalBinningProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}