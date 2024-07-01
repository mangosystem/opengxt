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

import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterShiftOperation;
import org.geotools.util.logging.Logging;

/**
 * Moves the raster to a new geographic location, based on x and y shift values.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterShiftProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterShiftProcess.class);

    public RasterShiftProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Double xShift,
            Double yShift, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterShiftProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterShiftProcessFactory.xShift.key, xShift);
        map.put(RasterShiftProcessFactory.yShift.key, yShift);

        Process process = new RasterShiftProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterShiftProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterShiftProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Double xShift = (Double) Params.getValue(input, RasterShiftProcessFactory.xShift,
                RasterShiftProcessFactory.xShift.sample);

        Double yShift = (Double) Params.getValue(input, RasterShiftProcessFactory.yShift,
                RasterShiftProcessFactory.yShift.sample);

        GridCoverage2D extractedGC = inputCoverage;

        if (xShift != 0d || yShift != 0d) {
            // start process
            RasterShiftOperation process = new RasterShiftOperation();
            extractedGC = process.execute(inputCoverage, xShift, yShift);
            // end process
        }

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterShiftProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
