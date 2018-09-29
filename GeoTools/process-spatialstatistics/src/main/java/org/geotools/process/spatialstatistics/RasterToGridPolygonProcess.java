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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.CoverageToGridFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Converts each pixel of a raster to grid polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToGridPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterToGridPolygonProcess.class);

    public RasterToGridPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Integer bandIndex,
            String valueField, ProgressListener monitor) {
        return process(inputCoverage, bandIndex, valueField, Boolean.FALSE, monitor);
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Integer bandIndex,
            String valueField, Boolean retainNoData, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterToGridPolygonProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterToGridPolygonProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterToGridPolygonProcessFactory.valueField.key, valueField);
        map.put(RasterToGridPolygonProcessFactory.retainNoData.key, retainNoData);

        Process process = new RasterToGridPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RasterToGridPolygonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterToGridPolygonProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Integer bandIndex = (Integer) Params.getValue(input,
                RasterToGridPolygonProcessFactory.bandIndex,
                RasterToGridPolygonProcessFactory.bandIndex.sample);
        String valueField = (String) Params.getValue(input,
                RasterToGridPolygonProcessFactory.valueField,
                RasterToGridPolygonProcessFactory.valueField.sample);
        Boolean retainNoData = (Boolean) Params.getValue(input,
                RasterToGridPolygonProcessFactory.retainNoData,
                RasterToGridPolygonProcessFactory.retainNoData.sample);

        // start process
        SimpleFeatureCollection resultFc = new CoverageToGridFeatureCollection(inputCoverage,
                bandIndex, valueField, retainNoData);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterToGridPolygonProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
