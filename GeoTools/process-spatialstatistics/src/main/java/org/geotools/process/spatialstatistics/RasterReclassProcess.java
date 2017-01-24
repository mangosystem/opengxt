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
import org.geotools.process.spatialstatistics.gridcoverage.RasterReclassOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Reclassifies a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReclassProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterReclassProcess.class);

    public RasterReclassProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Integer bandIndex,
            String ranges, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterReclassProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterReclassProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterReclassProcessFactory.ranges.key, ranges);

        Process process = new RasterReclassProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterReclassProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterReclassProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input, RasterReclassProcessFactory.bandIndex,
                RasterReclassProcessFactory.bandIndex.sample);
        String ranges = (String) Params.getValue(input, RasterReclassProcessFactory.ranges, null);
        if (inputCoverage == null || ranges == null || ranges.isEmpty()) {
            throw new NullPointerException("inputCoverage, ranges parameters required");
        }

        // start process
        RasterReclassOperation process = new RasterReclassOperation();
        GridCoverage2D cropedCoverage = process.execute(inputCoverage, bandIndex, ranges);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterReclassProcessFactory.RESULT.key, cropedCoverage);
        return resultMap;
    }
}
