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
 * RasterLinearLOSProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterLinearLOSProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterLinearLOSProcessFactory.class);

    private static final String PROCESS_NAME = "LinearLineOfSight";

    /*
     * LinearLineOfSight(GridCoverage2D inputCoverage, Geometry observerPoint, Double observerOffset, Geometry targetPoint, Boolean useCurvature,
     * Boolean useRefraction, Double refractionFactor) : SimpleFeatureCollection
     */

    public RasterLinearLOSProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterLinearLOSProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("LinearLineOfSight.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("LinearLineOfSight.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("LinearLineOfSight.inputCoverage.title"),
            getResource("LinearLineOfSight.inputCoverage.description"), true, 1, 1, null, null);

    /** observerPoint */
    public static final Parameter<Geometry> observerPoint = new Parameter<Geometry>(
            "observerPoint", Geometry.class, getResource("LinearLineOfSight.observerPoint.title"),
            getResource("LinearLineOfSight.observerPoint.description"), true, 1, 1, null, null);

    /** observerOffset */
    public static final Parameter<Double> observerOffset = new Parameter<Double>("observerOffset",
            Double.class, getResource("LinearLineOfSight.observerOffset.title"),
            getResource("LinearLineOfSight.observerOffset.description"), true, 1, 1,
            Double.valueOf(0d), null);

    /** targetPoint */
    public static final Parameter<Geometry> targetPoint = new Parameter<Geometry>("targetPoint",
            Geometry.class, getResource("LinearLineOfSight.targetPoint.title"),
            getResource("LinearLineOfSight.targetPoint.description"), true, 1, 1, null, null);

    /** useCurvature */
    public static final Parameter<Boolean> useCurvature = new Parameter<Boolean>("useCurvature",
            Boolean.class, getResource("LinearLineOfSight.useCurvature.title"),
            getResource("LinearLineOfSight.useCurvature.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** useRefraction */
    public static final Parameter<Boolean> useRefraction = new Parameter<Boolean>("useRefraction",
            Boolean.class, getResource("LinearLineOfSight.useRefraction.title"),
            getResource("LinearLineOfSight.useRefraction.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** refractionFactor */
    public static final Parameter<Double> refractionFactor = new Parameter<Double>(
            "refractionFactor", Double.class,
            getResource("LinearLineOfSight.refractionFactor.title"),
            getResource("LinearLineOfSight.refractionFactor.description"), false, 0, 1,
            Double.valueOf(0.13d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(observerPoint.key, observerPoint);
        parameterInfo.put(observerOffset.key, observerOffset);
        parameterInfo.put(targetPoint.key, targetPoint);
        parameterInfo.put(useCurvature.key, useCurvature);
        parameterInfo.put(useRefraction.key, useRefraction);
        parameterInfo.put(refractionFactor.key, refractionFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("LinearLineOfSight.result.title"),
            getResource("LinearLineOfSight.result.description"));

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
