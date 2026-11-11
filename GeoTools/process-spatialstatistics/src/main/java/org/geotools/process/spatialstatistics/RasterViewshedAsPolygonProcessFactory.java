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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/** Factory for statistics:ViewshedAsPolygon with the same inputs as statistics:Viewshed. */
public class RasterViewshedAsPolygonProcessFactory extends SpatialStatisticsProcessFactory {
    public RasterViewshedAsPolygonProcessFactory() {
        super(new NameImpl(NAMESPACE, "ViewshedAsPolygon"));
    }

    @Override
    public Process create() {
        return new RasterViewshedAsPolygonProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ViewshedAsPolygon.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ViewshedAsPolygon.description");
    }

    public static final Parameter<GridCoverage2D> inputCoverage = RasterViewshedProcessFactory.inputCoverage;
    public static final Parameter<Geometry> observerPoint = RasterViewshedProcessFactory.observerPoint;
    public static final Parameter<Double> observerOffset = RasterViewshedProcessFactory.observerOffset;
    public static final Parameter<Double> maxDistance = RasterViewshedProcessFactory.maxDistance;
    public static final Parameter<Boolean> useCurvature = RasterViewshedProcessFactory.useCurvature;
    public static final Parameter<Boolean> useRefraction = RasterViewshedProcessFactory.useRefraction;
    public static final Parameter<Double> refractionFactor = RasterViewshedProcessFactory.refractionFactor;

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

    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ViewshedAsPolygon.result.title"),
            getResource("ViewshedAsPolygon.result.description"));

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters) {
        return Collections.<String, Parameter<?>>singletonMap(RESULT.key, RESULT);
    }
}
