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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.InternationalString;

/**
 * RasterRadialLOSProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRadialLOSProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterRadialLOSProcessFactory.class);

    private static final String PROCESS_NAME = "RadialLineOfSight";

    /*
     * RadialLineOfSight(GridCoverage2D inputCoverage, Geometry observerPoint, Double observerOffset, Double radius, Integer sides, Boolean
     * useCurvature, Boolean useRefraction, Double refractionFactor) : SimpleFeatureCollection
     */

    public RasterRadialLOSProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterRadialLOSProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RadialLineOfSight.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RadialLineOfSight.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RadialLineOfSight.inputCoverage.title"),
            getResource("RadialLineOfSight.inputCoverage.description"), true, 1, 1, null, null);

    /** observerPoint */
    public static final Parameter<Geometry> observerPoint = new Parameter<Geometry>(
            "observerPoint", Geometry.class, getResource("RadialLineOfSight.observerPoint.title"),
            getResource("RadialLineOfSight.observerPoint.description"), true, 1, 1, null, null);

    /** observerOffset */
    public static final Parameter<Double> observerOffset = new Parameter<Double>("observerOffset",
            Double.class, getResource("RadialLineOfSight.observerOffset.title"),
            getResource("RadialLineOfSight.observerOffset.description"), true, 1, 1,
            Double.valueOf(0d), null);

    /** radius */
    public static final Parameter<Double> radius = new Parameter<Double>("radius", Double.class,
            getResource("RadialLineOfSight.radius.title"),
            getResource("RadialLineOfSight.radius.description"), true, 1, 1, Double.valueOf(0d),
            null);

    /** sides */
    public static final Parameter<Integer> sides = new Parameter<Integer>("sides", Integer.class,
            getResource("RadialLineOfSight.sides.title"),
            getResource("RadialLineOfSight.sides.description"), false, 0, 1, Integer.valueOf(180),
            null);

    /** useCurvature */
    public static final Parameter<Boolean> useCurvature = new Parameter<Boolean>("useCurvature",
            Boolean.class, getResource("RadialLineOfSight.useCurvature.title"),
            getResource("RadialLineOfSight.useCurvature.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** useRefraction */
    public static final Parameter<Boolean> useRefraction = new Parameter<Boolean>("useRefraction",
            Boolean.class, getResource("RadialLineOfSight.useRefraction.title"),
            getResource("RadialLineOfSight.useRefraction.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** refractionFactor */
    public static final Parameter<Double> refractionFactor = new Parameter<Double>(
            "refractionFactor", Double.class,
            getResource("RadialLineOfSight.refractionFactor.title"),
            getResource("RadialLineOfSight.refractionFactor.description"), false, 0, 1,
            Double.valueOf(0.13d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(observerPoint.key, observerPoint);
        parameterInfo.put(observerOffset.key, observerOffset);
        parameterInfo.put(radius.key, radius);
        parameterInfo.put(sides.key, sides);
        parameterInfo.put(useCurvature.key, useCurvature);
        parameterInfo.put(useRefraction.key, useRefraction);
        parameterInfo.put(refractionFactor.key, refractionFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RadialLineOfSight.result.title"),
            getResource("RadialLineOfSight.result.description"));

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
