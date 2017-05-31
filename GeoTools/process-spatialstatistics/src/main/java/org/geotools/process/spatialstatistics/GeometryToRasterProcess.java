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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.GeometryToRasterOperation;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Converts geometry to a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryToRasterProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GeometryToRasterProcess.class);

    public GeometryToRasterProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(Geometry inputGeometry,
            CoordinateReferenceSystem forcedCRS, Number defaultValue, RasterPixelType pixelType,
            Double cellSize, ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GeometryToRasterProcessFactory.inputGeometry.key, inputGeometry);
        map.put(GeometryToRasterProcessFactory.forcedCRS.key, forcedCRS);
        map.put(GeometryToRasterProcessFactory.defaultValue.key, defaultValue);
        map.put(GeometryToRasterProcessFactory.pixelType.key, pixelType);
        map.put(GeometryToRasterProcessFactory.cellSize.key, cellSize);
        map.put(GeometryToRasterProcessFactory.extent.key, extent);

        Process process = new GeometryToRasterProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(GeometryToRasterProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        Geometry inputGeometry = (Geometry) Params.getValue(input,
                GeometryToRasterProcessFactory.inputGeometry, null);
        CoordinateReferenceSystem forcedCRS = (CoordinateReferenceSystem) Params.getValue(input,
                GeometryToRasterProcessFactory.forcedCRS, null);
        if (inputGeometry == null) {
            throw new NullPointerException("inputGeometry parameter required");
        }

        // check CRS
        if (forcedCRS == null && inputGeometry.getUserData() != null) {
            if (inputGeometry.getUserData() instanceof CoordinateReferenceSystem) {
                forcedCRS = (CoordinateReferenceSystem) inputGeometry.getUserData();
            }
        }

        if (forcedCRS == null) {
            throw new NullPointerException("Geometry's CRS required!");
        }

        Number defaultValue = (Number) Params.getValue(input,
                GeometryToRasterProcessFactory.defaultValue,
                GeometryToRasterProcessFactory.defaultValue.sample);

        RasterPixelType pixelType = (RasterPixelType) Params.getValue(input,
                GeometryToRasterProcessFactory.pixelType,
                GeometryToRasterProcessFactory.pixelType.sample);

        Double cellSize = (Double) Params.getValue(input, GeometryToRasterProcessFactory.cellSize,
                GeometryToRasterProcessFactory.cellSize.sample);

        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                GeometryToRasterProcessFactory.extent, null);

        // start process
        if (extent == null) {
            extent = new ReferencedEnvelope(inputGeometry.getEnvelopeInternal(), forcedCRS);
        }

        // get default cell size from extent
        if (cellSize == null || cellSize == 0.0) {
            cellSize = Math.min(extent.getWidth(), extent.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        GeometryToRasterOperation process = new GeometryToRasterOperation();
        process.getRasterEnvironment().setCellSize(cellSize);
        process.getRasterEnvironment().setExtent(extent);

        GridCoverage2D resultGc = process
                .execute(inputGeometry, forcedCRS, defaultValue, pixelType);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(GeometryToRasterProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
