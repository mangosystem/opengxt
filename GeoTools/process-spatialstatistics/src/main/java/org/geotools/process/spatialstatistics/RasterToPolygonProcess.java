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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterToPolygonOperation;
import org.geotools.util.logging.Logging;

/**
 * Converts a raster dataset to polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterToPolygonProcess.class);

    public RasterToPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Integer bandIndex,
            Boolean weeding, String valueField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterToPolygonProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterToPolygonProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterToPolygonProcessFactory.weeding.key, weeding);
        map.put(RasterToPolygonProcessFactory.valueField.key, valueField);

        Process process = new RasterToPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RasterToPolygonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterToPolygonProcessFactory.inputCoverage, null);
        Integer bandIndex = (Integer) Params.getValue(input,
                RasterToPolygonProcessFactory.bandIndex,
                RasterToPolygonProcessFactory.bandIndex.sample);
        Boolean weeding = (Boolean) Params.getValue(input, RasterToPolygonProcessFactory.weeding,
                RasterToPolygonProcessFactory.weeding.sample);
        String valueField = (String) Params.getValue(input,
                RasterToPolygonProcessFactory.valueField,
                RasterToPolygonProcessFactory.valueField.sample);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        // start process
        SimpleFeatureCollection resultFc;
        try {
            RasterToPolygonOperation process = new RasterToPolygonOperation();
            resultFc = process.execute(inputCoverage, bandIndex, weeding, valueField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterToPolygonProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
