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

import org.geotools.api.filter.Filter;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterSetNullOperation;
import org.geotools.util.logging.Logging;

/**
 * Set Null sets identified cell locations to NoData based on a specified criteria.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSetNullProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterSetNullProcess.class);

    public RasterSetNullProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Integer bandIndex,
            Filter filter, Boolean replaceNoData, Double newValue, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterSetNullProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterSetNullProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterSetNullProcessFactory.filter.key, filter);
        map.put(RasterSetNullProcessFactory.replaceNoData.key, replaceNoData);
        map.put(RasterSetNullProcessFactory.newValue.key, newValue);

        Process process = new RasterSetNullProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterSetNullProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterSetNullProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input, RasterSetNullProcessFactory.bandIndex,
                RasterSetNullProcessFactory.bandIndex.sample);
        Filter filter = (Filter) Params.getValue(input, RasterSetNullProcessFactory.filter, null);
        if (inputCoverage == null || filter == null) {
            throw new NullPointerException("inputCoverage, filter parameters required");
        }

        Boolean replaceNoData = (Boolean) Params.getValue(input,
                RasterSetNullProcessFactory.replaceNoData,
                RasterSetNullProcessFactory.replaceNoData.sample);
        Double newValue = (Double) Params.getValue(input,
                RasterSetNullProcessFactory.newValue,
                RasterSetNullProcessFactory.newValue.sample);

        // start process
        RasterSetNullOperation process = new RasterSetNullOperation();
        GridCoverage2D cropedCoverage = process.execute(inputCoverage, bandIndex, filter,
                replaceNoData, newValue);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterSetNullProcessFactory.RESULT.key, cropedCoverage);
        return resultMap;
    }
}
