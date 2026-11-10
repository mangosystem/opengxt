/*
 * GeoTools - The Open Source Java GIS Toolkit
 * Copyright (C) 2026, Mango System inc.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.geotools.process.spatialstatistics;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedOperation;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/** Computes a single-observer visibility raster; all heights and distances are metres. */
public class RasterViewshedProcess extends AbstractStatisticsProcess {
    public RasterViewshedProcess(ProcessFactory factory) {
        super(factory);
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Geometry observerPoint,
            Double observerOffset, Double maxDistance, Boolean useCurvature, Boolean useRefraction,
            Double refractionFactor, ProgressListener monitor) {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put(RasterViewshedProcessFactory.inputCoverage.key, inputCoverage);
        input.put(RasterViewshedProcessFactory.observerPoint.key, observerPoint);
        input.put(RasterViewshedProcessFactory.observerOffset.key, observerOffset);
        input.put(RasterViewshedProcessFactory.maxDistance.key, maxDistance);
        input.put(RasterViewshedProcessFactory.useCurvature.key, useCurvature);
        input.put(RasterViewshedProcessFactory.useRefraction.key, useRefraction);
        input.put(RasterViewshedProcessFactory.refractionFactor.key, refractionFactor);
        return (GridCoverage2D) new RasterViewshedProcess(null).execute(input, monitor)
                .get(RasterViewshedProcessFactory.RESULT.key);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D coverage = (GridCoverage2D) Params.getValue(input,
                RasterViewshedProcessFactory.inputCoverage, null);
        Geometry observer = (Geometry) Params.getValue(input,
                RasterViewshedProcessFactory.observerPoint, null);
        if (coverage == null || !(observer instanceof Point) || observer.isEmpty()) {
            throw new ProcessException("inputCoverage and a non-empty Point observerPoint are required.");
        }
        double offset = (Double) Params.getValue(input, RasterViewshedProcessFactory.observerOffset, 1.6d);
        double radius = (Double) Params.getValue(input, RasterViewshedProcessFactory.maxDistance, 0d);
        boolean curvature = (Boolean) Params.getValue(input, RasterViewshedProcessFactory.useCurvature, false);
        boolean refraction = (Boolean) Params.getValue(input, RasterViewshedProcessFactory.useRefraction, false);
        double factor = (Double) Params.getValue(input, RasterViewshedProcessFactory.refractionFactor, .13d);
        observer = transformGeometry(observer, coverage.getCoordinateReferenceSystem2D());
        GridCoverage2D result = new RasterViewshedOperation().execute(coverage, observer.getCoordinate(),
                offset, radius, curvature, refraction, factor, monitor);
        Map<String, Object> output = new HashMap<String, Object>();
        output.put(RasterViewshedProcessFactory.RESULT.key, result);
        return output;
    }
}
