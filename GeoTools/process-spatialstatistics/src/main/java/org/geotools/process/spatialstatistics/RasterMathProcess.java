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

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterMathOperation;
import org.geotools.util.logging.Logging;

/**
 * Performs mathematical operations on raster using expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterMathProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterMathProcess.class);

    public RasterMathProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Integer bandIndex,
            Expression expression, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterMathProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterMathProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterMathProcessFactory.expression.key, expression);

        Process process = new RasterMathProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterMathProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterMathProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input, RasterMathProcessFactory.bandIndex,
                RasterMathProcessFactory.bandIndex.sample);
        Expression expression = (Expression) Params.getValue(input,
                RasterMathProcessFactory.expression, null);
        if (inputCoverage == null || expression == null) {
            throw new NullPointerException("inputCoverage, expression parameters required");
        }

        // start process
        RasterMathOperation process = new RasterMathOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, bandIndex, expression);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterMathProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
