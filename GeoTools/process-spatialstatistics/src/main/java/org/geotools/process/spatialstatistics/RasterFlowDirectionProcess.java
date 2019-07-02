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
import org.geotools.process.spatialstatistics.gridcoverage.RasterFlowDirectionOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a raster of flow direction from each cell to its downslope neighbor, or neighbors, using D8 methods.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFlowDirectionProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterFlowDirectionProcess.class);

    public RasterFlowDirectionProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterFlowDirectionProcessFactory.inputCoverage.key, inputCoverage);

        Process process = new RasterFlowDirectionProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterFlowDirectionProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterFlowDirectionProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        // start process
        RasterFlowDirectionOperation process = new RasterFlowDirectionOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterFlowDirectionProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
