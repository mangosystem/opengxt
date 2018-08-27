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
import org.geotools.process.spatialstatistics.gridcoverage.RasterNDVIOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Derives Normalized Difference Vegetation Index (NDVI) from two rasters.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterNDVIProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterNDVIProcess.class);

    public RasterNDVIProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D nirCoverage, Integer nirIndex,
            GridCoverage2D redCoverage, Integer redIndex, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterNDVIProcessFactory.nirCoverage.key, nirCoverage);
        map.put(RasterNDVIProcessFactory.nirIndex.key, nirIndex);
        map.put(RasterNDVIProcessFactory.redCoverage.key, redCoverage);
        map.put(RasterNDVIProcessFactory.redIndex.key, redIndex);

        Process process = new RasterNDVIProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterNDVIProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D nirCoverage = (GridCoverage2D) Params.getValue(input,
                RasterNDVIProcessFactory.nirCoverage, null);
        Integer nirIndex = (Integer) Params.getValue(input, RasterNDVIProcessFactory.nirIndex,
                RasterNDVIProcessFactory.nirIndex.sample);
        GridCoverage2D redCoverage = (GridCoverage2D) Params.getValue(input,
                RasterNDVIProcessFactory.redCoverage, null);
        Integer redIndex = (Integer) Params.getValue(input, RasterNDVIProcessFactory.redIndex,
                RasterNDVIProcessFactory.redIndex.sample);
        if (nirCoverage == null) {
            throw new NullPointerException("nirCoverage, expression parameters required");
        }

        // start process
        RasterNDVIOperation process = new RasterNDVIOperation();
        GridCoverage2D extractedGC = process.execute(nirCoverage, nirIndex, redCoverage, redIndex);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterNDVIProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
