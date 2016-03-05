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
import org.geotools.geometry.jts.JTS;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterClipOperation;
import org.geotools.referencing.CRS;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Extracts the subset of a raster based on a polygon geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterClipByGeometryProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterClipByGeometryProcess.class);

    private boolean started = false;

    public RasterClipByGeometryProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Geometry cropShape,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterClipByGeometryProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterClipByGeometryProcessFactory.cropShape.key, cropShape);

        Process process = new RasterClipByGeometryProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterClipByGeometryProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                    RasterClipByGeometryProcessFactory.inputCoverage, null);
            Geometry cropShape = (Geometry) Params.getValue(input,
                    RasterClipByGeometryProcessFactory.cropShape, null);
            if (inputCoverage == null || cropShape == null) {
                throw new NullPointerException("inputCoverage, cropShape parameters required");
            }

            monitor.setTask(Text.text("Processing Statistics"));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            Object userData = cropShape.getUserData();
            CoordinateReferenceSystem tCrs = inputCoverage.getCoordinateReferenceSystem();
            if (userData == null) {
                cropShape.setUserData(tCrs);
            } else if (userData instanceof CoordinateReferenceSystem) {
                CoordinateReferenceSystem sCrs = (CoordinateReferenceSystem) userData;
                if (!CRS.equalsIgnoreMetadata(sCrs, tCrs)) {
                    try {
                        MathTransform transform = CRS.findMathTransform(sCrs, tCrs, true);
                        cropShape = JTS.transform(cropShape, transform);
                        cropShape.setUserData(tCrs);
                    } catch (FactoryException e) {
                        throw new IllegalArgumentException("Could not create math transform");
                    }
                }
            }

            RasterClipOperation cropOperation = new RasterClipOperation();
            GridCoverage2D cropedCoverage = cropOperation.execute(inputCoverage, cropShape);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(RasterClipByGeometryProcessFactory.RESULT.key, cropedCoverage);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }
}
