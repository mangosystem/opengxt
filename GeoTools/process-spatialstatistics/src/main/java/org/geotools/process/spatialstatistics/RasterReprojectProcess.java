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
import org.geotools.process.spatialstatistics.gridcoverage.RasterReprojectOperation;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Reprojects the raster dataset from one projection to another.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReprojectProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterReprojectProcess.class);

    public RasterReprojectProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, Double cellSize,
            CoordinateReferenceSystem forcedCRS, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterReprojectProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterReprojectProcessFactory.targetCRS.key, targetCRS);
        map.put(RasterReprojectProcessFactory.resamplingType.key, resamplingType);
        map.put(RasterReprojectProcessFactory.cellSize.key, cellSize);
        map.put(RasterReprojectProcessFactory.forcedCRS.key, forcedCRS);

        Process process = new RasterReprojectProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterReprojectProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterReprojectProcessFactory.inputCoverage, null);
        CoordinateReferenceSystem targetCRS = (CoordinateReferenceSystem) Params.getValue(input,
                RasterReprojectProcessFactory.targetCRS, null);
        if (inputCoverage == null || targetCRS == null) {
            throw new NullPointerException("inputCoverage, targetCRS parameters required");
        }

        ResampleType resamplingType = (ResampleType) Params.getValue(input,
                RasterReprojectProcessFactory.resamplingType,
                RasterReprojectProcessFactory.resamplingType.sample);

        Double cellSize = (Double) Params.getValue(input, RasterReprojectProcessFactory.cellSize,
                RasterReprojectProcessFactory.cellSize.sample);

        CoordinateReferenceSystem forcedCRS = (CoordinateReferenceSystem) Params.getValue(input,
                RasterReprojectProcessFactory.forcedCRS, null);

        // start process
        RasterReprojectOperation process = new RasterReprojectOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, targetCRS, resamplingType,
                cellSize, cellSize, forcedCRS);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterReprojectProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
