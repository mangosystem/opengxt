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
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterResampleOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Change the spatial resolution of raster and set rules for aggregating or interpolating values across the new pixel sizes.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterResampleProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterResampleProcess.class);

    public RasterResampleProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Double cellSize,
            ResampleType resamplingType, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterResampleProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterResampleProcessFactory.cellSize.key, cellSize);
        map.put(RasterResampleProcessFactory.resamplingType.key, resamplingType);

        Process process = new RasterResampleProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterResampleProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterResampleProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Double cellSize = (Double) Params.getValue(input, RasterResampleProcessFactory.cellSize,
                RasterResampleProcessFactory.cellSize.sample);
        if (cellSize == null || cellSize <= 0 || cellSize.isNaN()) {
            throw new NullPointerException("cellSize parameter required");
        }

        ResampleType resamplingType = (ResampleType) Params.getValue(input,
                RasterResampleProcessFactory.resamplingType,
                RasterResampleProcessFactory.resamplingType.sample);

        // start process
        RasterResampleOperation process = new RasterResampleOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, cellSize, cellSize, resamplingType);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterResampleProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
