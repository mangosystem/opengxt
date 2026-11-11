/*
 * GeoTools - The Open Source Java GIS Toolkit
 * Copyright (C) 2026, Mango System inc.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.geotools.process.spatialstatistics;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.gridcoverage.RasterViewshedAsPolygonOperation;
import org.geotools.util.SubProgressListener;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/** Computes a viewshed and returns connected visible/invisible areas as polygons. */
public class RasterViewshedAsPolygonProcess extends AbstractStatisticsProcess {
    public RasterViewshedAsPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Geometry observerPoint,
            Double observerOffset, Double maxDistance, Boolean useCurvature, Boolean useRefraction,
            Double refractionFactor, ProgressListener monitor) {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put(RasterViewshedAsPolygonProcessFactory.inputCoverage.key, inputCoverage);
        input.put(RasterViewshedAsPolygonProcessFactory.observerPoint.key, observerPoint);
        input.put(RasterViewshedAsPolygonProcessFactory.observerOffset.key, observerOffset);
        input.put(RasterViewshedAsPolygonProcessFactory.maxDistance.key, maxDistance);
        input.put(RasterViewshedAsPolygonProcessFactory.useCurvature.key, useCurvature);
        input.put(RasterViewshedAsPolygonProcessFactory.useRefraction.key, useRefraction);
        input.put(RasterViewshedAsPolygonProcessFactory.refractionFactor.key, refractionFactor);
        return (SimpleFeatureCollection) new RasterViewshedAsPolygonProcess(null).execute(input, monitor)
                .get(RasterViewshedAsPolygonProcessFactory.RESULT.key);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (monitor != null) monitor.started();
        ProgressListener rasterMonitor = monitor == null ? null : new SubProgressListener(monitor, 0, 80);
        ProgressListener polygonMonitor = monitor == null ? null : new SubProgressListener(monitor, 80, 20);
        GridCoverage2D visibility = (GridCoverage2D) new RasterViewshedProcess(null)
                .execute(input, rasterMonitor).get(RasterViewshedProcessFactory.RESULT.key);
        try {
            SimpleFeatureCollection polygons = new RasterViewshedAsPolygonOperation()
                    .execute(visibility, polygonMonitor);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put(RasterViewshedAsPolygonProcessFactory.RESULT.key, polygons);
            if (monitor != null) monitor.complete();
            return result;
        } finally {
            // Polygons are fully materialized; only this intermediate coverage is owned here.
            visibility.dispose(true);
        }
    }
}
