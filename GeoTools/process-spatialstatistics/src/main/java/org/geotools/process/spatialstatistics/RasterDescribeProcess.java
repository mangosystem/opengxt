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
import org.geotools.process.spatialstatistics.gridcoverage.RasterDescribeOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterDescribeOperation.RasterDescribeResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Describes the metadata of gridcoverage.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterDescribeProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterDescribeProcess.class);

    public RasterDescribeProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Boolean detailed,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterDescribeProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterDescribeProcessFactory.detailed.key, detailed);

        Process process = new RasterDescribeProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterDescribeProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterDescribeProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Boolean detailed = (Boolean) Params.getValue(input, RasterDescribeProcessFactory.detailed,
                RasterDescribeProcessFactory.detailed.sample);

        // start process
        RasterDescribeOperation process = new RasterDescribeOperation();
        RasterDescribeResult description = process.execute(inputCoverage, detailed);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterDescribeProcessFactory.RESULT.key, description);
        return resultMap;
    }
}
