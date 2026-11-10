/*
 * GeoTools - The Open Source Java GIS Toolkit
 * Copyright (C) 2026, Mango System inc.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.geotools.process.spatialstatistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/** Factory for statistics:Viewshed. */
public class RasterViewshedProcessFactory extends SpatialStatisticsProcessFactory {
    public RasterViewshedProcessFactory() {
        super(new NameImpl(NAMESPACE, "Viewshed"));
    }

    @Override
    public Process create() {
        return new RasterViewshedProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Viewshed.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Viewshed.description");
    }

    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("Viewshed.inputCoverage.title"),
            getResource("Viewshed.inputCoverage.description"), true, 1, 1, null, null);

    public static final Parameter<Geometry> observerPoint = new Parameter<Geometry>(
            "observerPoint", Geometry.class, getResource("Viewshed.observerPoint.title"),
            getResource("Viewshed.observerPoint.description"), true, 1, 1, null, null);

    public static final Parameter<Double> observerOffset = new Parameter<Double>(
            "observerOffset", Double.class, getResource("Viewshed.observerOffset.title"),
            getResource("Viewshed.observerOffset.description"), false, 0, 1, 1.6d, null);

    public static final Parameter<Double> maxDistance = new Parameter<Double>(
            "maxDistance", Double.class, getResource("Viewshed.maxDistance.title"),
            getResource("Viewshed.maxDistance.description"), false, 0, 1, 0d, null);

    public static final Parameter<Boolean> useCurvature = new Parameter<Boolean>(
            "useCurvature", Boolean.class, getResource("Viewshed.useCurvature.title"),
            getResource("Viewshed.useCurvature.description"), false, 0, 1, Boolean.FALSE, null);

    public static final Parameter<Boolean> useRefraction = new Parameter<Boolean>(
            "useRefraction", Boolean.class, getResource("Viewshed.useRefraction.title"),
            getResource("Viewshed.useRefraction.description"), false, 0, 1, Boolean.FALSE, null);

    public static final Parameter<Double> refractionFactor = new Parameter<Double>(
            "refractionFactor", Double.class, getResource("Viewshed.refractionFactor.title"),
            getResource("Viewshed.refractionFactor.description"), false, 0, 1, .13d, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        Map<String, Parameter<?>> parameters = new LinkedHashMap<String, Parameter<?>>();
        parameters.put(inputCoverage.key, inputCoverage);
        parameters.put(observerPoint.key, observerPoint);
        parameters.put(observerOffset.key, observerOffset);
        parameters.put(maxDistance.key, maxDistance);
        parameters.put(useCurvature.key, useCurvature);
        parameters.put(useRefraction.key, useRefraction);
        parameters.put(refractionFactor.key, refractionFactor);
        return parameters;
    }

    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>(
            "result", GridCoverage2D.class, getResource("Viewshed.result.title"),
            getResource("Viewshed.result.description"));

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters) {
        return Collections.<String, Parameter<?>>singletonMap(RESULT.key, RESULT);
    }
}
