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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterClipOperation;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

/**
 * Extracts the subset of a raster based on a reference envelope.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterClipByExtentProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterClipByExtentProcess.class);

    private boolean started = false;

    public RasterClipByExtentProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, ReferencedEnvelope extent,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterClipByExtentProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterClipByExtentProcessFactory.extent.key, extent);

        Process process = new RasterClipByExtentProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterClipByExtentProcessFactory.RESULT.key);
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

        try {
            GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                    RasterClipByExtentProcessFactory.inputCoverage, null);
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    RasterClipByExtentProcessFactory.extent, null);
            if (inputCoverage == null || extent == null) {
                throw new NullPointerException("inputCoverage, extent parameters required");
            }

            // start process
            CoordinateReferenceSystem tCrs = inputCoverage.getCoordinateReferenceSystem();
            CoordinateReferenceSystem sCrs = extent.getCoordinateReferenceSystem();
            if (sCrs == null) {
                extent = new ReferencedEnvelope(extent, tCrs);
            } else if (!CRS.equalsIgnoreMetadata(sCrs, tCrs)) {
                try {
                    MathTransform transform = CRS.findMathTransform(sCrs, tCrs, true);
                    extent = new ReferencedEnvelope(JTS.transform(extent, transform), tCrs);
                } catch (FactoryException e) {
                    throw new IllegalArgumentException("Could not create math transform");
                }
            }

            RasterClipOperation cropOperation = new RasterClipOperation();
            GridCoverage2D cropedCoverage = cropOperation.execute(inputCoverage, extent);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(RasterClipByExtentProcessFactory.RESULT.key, cropedCoverage);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
