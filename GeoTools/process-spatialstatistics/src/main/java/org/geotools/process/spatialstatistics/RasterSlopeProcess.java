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
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterSlopeOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Identifies the slope (gradient, or rate of maximum change in z-value) from each cell of a raster surface.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSlopeProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterSlopeProcess.class);

    public RasterSlopeProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, SlopeType slopeType,
            double zFactor, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterSlopeProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterSlopeProcessFactory.slopeType.key, slopeType);
        map.put(RasterSlopeProcessFactory.zFactor.key, zFactor);

        Process process = new RasterSlopeProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterSlopeProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterSlopeProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        SlopeType slopeType = (SlopeType) Params.getValue(input,
                RasterSlopeProcessFactory.slopeType, RasterSlopeProcessFactory.slopeType.sample);

        Double zFactor = (Double) Params.getValue(input, RasterSlopeProcessFactory.zFactor,
                RasterSlopeProcessFactory.zFactor.sample);

        // start process
        RasterSlopeOperation process = new RasterSlopeOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, slopeType, zFactor);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterSlopeProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
