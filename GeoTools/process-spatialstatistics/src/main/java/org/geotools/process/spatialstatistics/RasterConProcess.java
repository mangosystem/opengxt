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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterConditionalOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Performs a conditional if/else evaluation on each of the input cells of an input raster.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterConProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterConProcess.class);

    public RasterConProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Integer bandIndex,
            Filter filter, Integer trueValue, Integer falseValue, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterConProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterConProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterConProcessFactory.filter.key, filter);
        map.put(RasterConProcessFactory.trueValue.key, trueValue);
        map.put(RasterConProcessFactory.falseValue.key, falseValue);

        Process process = new RasterConProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterConProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterConProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input, RasterConProcessFactory.bandIndex,
                RasterConProcessFactory.bandIndex.sample);
        Filter filter = (Filter) Params.getValue(input, RasterConProcessFactory.filter, null);
        Integer trueValue = (Integer) Params.getValue(input, RasterConProcessFactory.trueValue,
                RasterConProcessFactory.trueValue.sample);
        Integer falseValue = (Integer) Params.getValue(input, RasterConProcessFactory.falseValue,
                RasterConProcessFactory.falseValue.sample);
        if (inputCoverage == null || filter == null) {
            throw new NullPointerException("inputCoverage, filter parameters required");
        }

        // start process
        RasterConditionalOperation process = new RasterConditionalOperation();
        GridCoverage2D cropedCoverage = process.execute(inputCoverage, bandIndex, filter,
                trueValue, falseValue);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterConProcessFactory.RESULT.key, cropedCoverage);
        return resultMap;
    }
}
