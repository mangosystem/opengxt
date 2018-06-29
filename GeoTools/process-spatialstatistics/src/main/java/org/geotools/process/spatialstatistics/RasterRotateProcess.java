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

import javax.media.jai.Interpolation;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterRotateOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Point;

/**
 * The pivot point around which to rotate the raster. The default is the lower left corner of the input raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRotateProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterRotateProcess.class);

    public RasterRotateProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Point anchorPoint,
            Double angle, ResampleType interpolation, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterRotateProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterRotateProcessFactory.anchorPoint.key, anchorPoint);
        map.put(RasterRotateProcessFactory.angle.key, angle);
        map.put(RasterRotateProcessFactory.interpolation.key, interpolation);

        Process process = new RasterRotateProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterRotateProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterRotateProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Point anchorPoint = (Point) Params.getValue(input, RasterRotateProcessFactory.anchorPoint,
                null);

        Double angle = (Double) Params.getValue(input, RasterRotateProcessFactory.angle,
                RasterRotateProcessFactory.angle.sample);

        ResampleType resample = (ResampleType) Params.getValue(input,
                RasterRotateProcessFactory.interpolation,
                RasterRotateProcessFactory.interpolation.sample);

        // start process
        GridCoverage2D rotatedGC = inputCoverage;
        if (angle > 0) {
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            switch (resample) {
            case BICUBIC:
                interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
                break;
            case BILINEAR:
                interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                break;
            default:
                interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                break;
            }

            RasterRotateOperation process = new RasterRotateOperation();
            process.setInterpolation(interpolation);

            if (anchorPoint == null) {
                rotatedGC = process.execute(inputCoverage, angle);
            } else {
                rotatedGC = process.execute(inputCoverage, anchorPoint, angle);
            }
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterRotateProcessFactory.RESULT.key, rotatedGC);
        return resultMap;
    }
}
