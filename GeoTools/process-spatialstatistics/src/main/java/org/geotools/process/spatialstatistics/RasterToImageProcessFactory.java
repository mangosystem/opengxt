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

import org.geotools.api.data.Parameter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.MapToImageParam;
import org.geotools.util.logging.Logging;

/**
 * RasterToImageProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToImageProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterToImageProcessFactory.class);

    private static final String PROCESS_NAME = "RasterToImage";

    /*
     * RasterToImage(GridCoverage2D coverage, String bbox, CoordinateReferenceSystem crs, Style style, Integer width, Integer height, String format,
     * Boolean transparent, String bgColor): Image
     */

    public RasterToImageProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterToImageProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterToImage.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterToImage.description");
    }

    /** coverage */
    public static final Parameter<GridCoverage2D> coverage = new Parameter<GridCoverage2D>(
            "coverage", GridCoverage2D.class, getResource("RasterToImage.coverage.title"),
            getResource("RasterToImage.coverage.description"), true, 1, 1, null, null);

    /** bbox */
    public static final Parameter<String> bbox = new Parameter<String>("bbox", String.class,
            getResource("RasterToImage.bbox.title"), getResource("RasterToImage.bbox.description"),
            false, 0, 1, null, null);

    /** crs */
    public static final Parameter<CoordinateReferenceSystem> crs = new Parameter<CoordinateReferenceSystem>(
            "crs", CoordinateReferenceSystem.class, getResource("RasterToImage.crs.title"),
            getResource("RasterToImage.crs.description"), false, 0, 1, null, null);

    /** style */
    public static final Parameter<Style> style = new Parameter<Style>("style", Style.class,
            getResource("RasterToImage.style.title"),
            getResource("RasterToImage.style.description"), false, 0, 1, null, null);

    /** width */
    public static final Parameter<Integer> width = new Parameter<Integer>("width", Integer.class,
            getResource("RasterToImage.width.title"),
            getResource("RasterToImage.width.description"), true, 1, 1, null, null);

    /** height */
    public static final Parameter<Integer> height = new Parameter<Integer>("height", Integer.class,
            getResource("RasterToImage.height.title"),
            getResource("RasterToImage.height.description"), true, 1, 1, null, null);

    /** format */
    public static final Parameter<String> format = new Parameter<String>("format", String.class,
            getResource("RasterToImage.format.title"),
            getResource("RasterToImage.format.description"), false, 0, 1, "image/png", null);

    /** transparent */
    public static final Parameter<Boolean> transparent = new Parameter<Boolean>("transparent",
            Boolean.class, getResource("RasterToImage.transparent.title"),
            getResource("RasterToImage.transparent.description"), false, 0, 1, Boolean.TRUE, null);

    /** bgColor */
    public static final Parameter<String> bgColor = new Parameter<String>("bgColor", String.class,
            getResource("RasterToImage.bgColor.title"),
            getResource("RasterToImage.bgColor.description"), false, 0, 1, "0xFFFFFF", null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(coverage.key, coverage);
        parameterInfo.put(bbox.key, bbox);
        parameterInfo.put(crs.key, crs);
        parameterInfo.put(style.key, style);
        parameterInfo.put(width.key, width);
        parameterInfo.put(height.key, height);
        parameterInfo.put(format.key, format);
        parameterInfo.put(transparent.key, transparent);
        parameterInfo.put(bgColor.key, bgColor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<MapToImageParam> RESULT = new Parameter<MapToImageParam>(
            "result", MapToImageParam.class, getResource("RasterToImage.result.title"),
            getResource("RasterToImage.result.description"));

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
