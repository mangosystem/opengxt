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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * GeometryToRasterProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryToRasterProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(GeometryToRasterProcessFactory.class);

    private static final String PROCESS_NAME = "GeometryToRaster";

    /*
     * GeometryToRaster(Geometry inputGeometry, CoordinateReferenceSystem forcedCRS, Number defaultValue, RasterPixelType pixelType, Double cellSize,
     * ReferencedEnvelope extent): GridCoverage2D
     */

    public GeometryToRasterProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new GeometryToRasterProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("GeometryToRaster.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("GeometryToRaster.description");
    }

    /** inputGeometry */
    public static final Parameter<Geometry> inputGeometry = new Parameter<Geometry>(
            "inputGeometry", Geometry.class, getResource("GeometryToRaster.inputGeometry.title"),
            getResource("GeometryToRaster.inputGeometry.description"), true, 1, 1, null, null);

    /** forcedCRS */
    public static final Parameter<CoordinateReferenceSystem> forcedCRS = new Parameter<CoordinateReferenceSystem>(
            "forcedCRS", CoordinateReferenceSystem.class,
            getResource("GeometryToRaster.forcedCRS.title"),
            getResource("GeometryToRaster.forcedCRS.description"), false, 0, 1, null, null);

    /** defaultValue */
    public static final Parameter<Number> defaultValue = new Parameter<Number>("defaultValue",
            Number.class, getResource("GeometryToRaster.defaultValue.title"),
            getResource("GeometryToRaster.defaultValue.description"), false, 0, 1, 1, null);

    /** pixelType */
    public static final Parameter<RasterPixelType> pixelType = new Parameter<RasterPixelType>(
            "pixelType", RasterPixelType.class, getResource("GeometryToRaster.pixelType.title"),
            getResource("GeometryToRaster.pixelType.description"), false, 0, 1,
            RasterPixelType.INTEGER, null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("GeometryToRaster.cellSize.title"),
            getResource("GeometryToRaster.cellSize.description"), false, 0, 1, Double.valueOf(0.0),
            null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("GeometryToRaster.extent.title"),
            getResource("GeometryToRaster.extent.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputGeometry.key, inputGeometry);
        parameterInfo.put(forcedCRS.key, forcedCRS);
        parameterInfo.put(defaultValue.key, defaultValue);
        parameterInfo.put(pixelType.key, pixelType);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(extent.key, extent);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("GeometryToRaster.result.title"),
            getResource("GeometryToRaster.result.description"));

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(RESULT.key, RESULT);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
