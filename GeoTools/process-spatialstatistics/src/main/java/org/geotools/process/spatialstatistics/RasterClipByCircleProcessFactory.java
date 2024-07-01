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
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * RasterClipByCircleProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterClipByCircleProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterClipByCircleProcessFactory.class);

    private static final String PROCESS_NAME = "RasterClipByCircle";

    /*
     * RasterClipByCircle(GridCoverage2D inputCoverage, Geometry center, Double radius, Boolean inside): GridCoverage2D
     */

    public RasterClipByCircleProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterClipByCircleProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterClipByCircle.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterClipByCircle.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterClipByCircle.inputCoverage.title"),
            getResource("RasterClipByCircle.inputCoverage.description"), true, 1, 1, null, null);

    /** center */
    public static final Parameter<Geometry> center = new Parameter<Geometry>("center",
            Geometry.class, getResource("RasterClipByCircle.center.title"),
            getResource("RasterClipByCircle.center.description"), true, 1, 1, null, null);

    /** radius */
    public static final Parameter<Double> radius = new Parameter<Double>("radius", Double.class,
            getResource("RasterClipByCircle.radius.title"),
            getResource("RasterClipByCircle.radius.description"), true, 1, 1, Double.valueOf(0d),
            null);

    /** inside */
    public static final Parameter<Boolean> inside = new Parameter<Boolean>("inside", Boolean.class,
            getResource("RasterClipByCircle.inside.title"),
            getResource("RasterClipByCircle.inside.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(center.key, center);
        parameterInfo.put(radius.key, radius);
        parameterInfo.put(inside.key, inside);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterClipByCircle.result.title"),
            getResource("RasterClipByCircle.result.description"));

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
