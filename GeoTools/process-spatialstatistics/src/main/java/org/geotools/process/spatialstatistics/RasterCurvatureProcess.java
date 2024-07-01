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
import org.geotools.process.spatialstatistics.gridcoverage.RasterCurvatureOperation;
import org.geotools.util.logging.Logging;

/**
 * Calculates the curvature of a raster surface.
 * <p>
 * Moore, I. D., R. B. Grayson, and A. R. Landson. 1991. Digital Terrain Modelling: A Review of Hydrological, Geomorphological, and Biological
 * Applications. Hydrological Processes 5: 3–30.
 * <p>
 * Zeverbergen, L. W., and C. R. Thorne. 1987. Quantitative Analysis of Land Surface Topography. Earth Surface Processes and Landforms 12: 47–56.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCurvatureProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterCurvatureProcess.class);

    public RasterCurvatureProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, double zFactor,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterCurvatureProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterCurvatureProcessFactory.zFactor.key, zFactor);

        Process process = new RasterCurvatureProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterCurvatureProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterCurvatureProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Double zFactor = (Double) Params.getValue(input, RasterCurvatureProcessFactory.zFactor,
                RasterCurvatureProcessFactory.zFactor.sample);

        // start process
        RasterCurvatureOperation process = new RasterCurvatureOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, zFactor);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterCurvatureProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
