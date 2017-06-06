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
import org.geotools.process.spatialstatistics.gridcoverage.RasterExtractionOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Extracts the cells of a raster based on a logical query.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExtractionProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterExtractionProcess.class);

    public RasterExtractionProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Integer bandIndex,
            Filter filter, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterExtractionProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterExtractionProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterExtractionProcessFactory.filter.key, filter);

        Process process = new RasterExtractionProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterExtractionProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterExtractionProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input,
                RasterExtractionProcessFactory.bandIndex,
                RasterExtractionProcessFactory.bandIndex.sample);
        Filter filter = (Filter) Params
                .getValue(input, RasterExtractionProcessFactory.filter, null);
        if (inputCoverage == null || filter == null) {
            throw new NullPointerException("inputCoverage, filter parameters required");
        }

        // start process
        RasterExtractionOperation process = new RasterExtractionOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, bandIndex, filter);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterExtractionProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
