/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.process.spatialstatistics.gridcoverage.RasterReplaceValuesOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * Replace raster values within polygon with specific value.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReplaceValuesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterReplaceValuesProcess.class);

    public RasterReplaceValuesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Geometry region,
            Double replaceValue, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterReplaceValuesProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterReplaceValuesProcessFactory.region.key, region);
        map.put(RasterReplaceValuesProcessFactory.replaceValue.key, replaceValue);

        Process process = new RasterReplaceValuesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterReplaceValuesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterReplaceValuesProcessFactory.inputCoverage, null);
        Geometry region = (Geometry) Params.getValue(input,
                RasterReplaceValuesProcessFactory.region, null);
        Double replaceValue = (Double) Params.getValue(input,
                RasterReplaceValuesProcessFactory.replaceValue, null);
        if (inputCoverage == null || region == null || replaceValue == null) {
            throw new NullPointerException(
                    "inputCoverage, region, replaceValue parameters required");
        }

        // start process
        RasterReplaceValuesOperation process = new RasterReplaceValuesOperation();
        GridCoverage2D resultGc = process.execute(inputCoverage, region, replaceValue);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterReplaceValuesProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
