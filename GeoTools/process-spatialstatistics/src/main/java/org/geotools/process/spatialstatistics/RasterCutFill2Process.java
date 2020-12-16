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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterCutFillOperation2;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.ProgressListener;

/**
 * Calculates the volume change between two surfaces.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFill2Process extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFill2Process.class);

    public RasterCutFill2Process(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D beforeCoverage,
            GridCoverage2D afterCoverage, Geometry cropShape, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterCutFill2ProcessFactory.beforeCoverage.key, beforeCoverage);
        map.put(RasterCutFill2ProcessFactory.afterCoverage.key, afterCoverage);
        map.put(RasterCutFill2ProcessFactory.cropShape.key, cropShape);

        Process process = new RasterCutFill2Process(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(RasterCutFill2ProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D beforeCoverage = (GridCoverage2D) Params.getValue(input,
                RasterCutFill2ProcessFactory.beforeCoverage, null);
        GridCoverage2D afterCoverage = (GridCoverage2D) Params.getValue(input,
                RasterCutFill2ProcessFactory.afterCoverage, null);
        Geometry cropShape = (Geometry) Params.getValue(input,
                RasterCutFill2ProcessFactory.cropShape, null);
        if (beforeCoverage == null || afterCoverage == null || cropShape == null) {
            throw new NullPointerException(
                    "beforeCoverage, afterCoverage, cropShape parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            RasterCutFillOperation2 cutfillOperation = new RasterCutFillOperation2();
            resultFc = cutfillOperation.execute(beforeCoverage, afterCoverage, cropShape);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterCutFill2ProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
