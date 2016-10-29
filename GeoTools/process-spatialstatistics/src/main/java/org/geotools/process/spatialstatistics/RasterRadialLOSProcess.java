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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterFunctionalSurface;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

/**
 * Determines the visibility a surface within a specified radius and field of view of an observation point.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRadialLOSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterRadialLOSProcess.class);

    private boolean started = false;

    static final String ANGLE_FIELD = "Angle";

    static final String VALUE_FIELD = "Visible";

    public RasterRadialLOSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage,
            Geometry observerPoint, Double observerOffset, Double radius, Integer sides,
            Boolean useCurvature, Boolean useRefraction, Double refractionFactor,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterRadialLOSProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterRadialLOSProcessFactory.observerPoint.key, observerPoint);
        map.put(RasterRadialLOSProcessFactory.observerOffset.key, observerOffset);
        map.put(RasterRadialLOSProcessFactory.radius.key, radius);
        map.put(RasterRadialLOSProcessFactory.sides.key, sides);
        map.put(RasterRadialLOSProcessFactory.useCurvature.key, useCurvature);
        map.put(RasterRadialLOSProcessFactory.useRefraction.key, useRefraction);
        map.put(RasterRadialLOSProcessFactory.refractionFactor.key, refractionFactor);

        Process process = new RasterRadialLOSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RasterRadialLOSProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                    RasterRadialLOSProcessFactory.inputCoverage, null);
            Geometry observerPoint = (Geometry) Params.getValue(input,
                    RasterRadialLOSProcessFactory.observerPoint, null);
            Double observerOffset = (Double) Params.getValue(input,
                    RasterRadialLOSProcessFactory.observerOffset,
                    RasterRadialLOSProcessFactory.observerOffset.sample);
            Double radius = (Double) Params.getValue(input, RasterRadialLOSProcessFactory.radius,
                    RasterRadialLOSProcessFactory.radius.sample);
            Integer sides = (Integer) Params.getValue(input, RasterRadialLOSProcessFactory.sides,
                    RasterRadialLOSProcessFactory.sides.sample);
            Boolean useCurvature = (Boolean) Params.getValue(input,
                    RasterRadialLOSProcessFactory.useCurvature,
                    RasterRadialLOSProcessFactory.useCurvature.sample);
            Boolean useRefraction = (Boolean) Params.getValue(input,
                    RasterRadialLOSProcessFactory.useRefraction,
                    RasterRadialLOSProcessFactory.useRefraction.sample);
            Double refractionFactor = (Double) Params.getValue(input,
                    RasterRadialLOSProcessFactory.refractionFactor,
                    RasterRadialLOSProcessFactory.refractionFactor.sample);

            if (inputCoverage == null || observerPoint == null || radius == null) {
                throw new NullPointerException(
                        "inputCoverage, observerPoint, radius parameters required");
            }

            if (radius <= 0 || observerOffset < 0 || sides <= 0) {
                throw new NullPointerException(
                        "radius, observerOffset, sides parameters must be a positive value");
            }

            // start process
            CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
            SimpleFeatureType featureType = FeatureTypes.getDefaultType("RadialLineOfSight",
                    LineString.class, crs);
            featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
            featureType = FeatureTypes.add(featureType, VALUE_FIELD, Integer.class, 5);

            // prepare transactional feature store
            ListFeatureCollection resultSfc = new ListFeatureCollection(featureType);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

            RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
            Coordinate center = observerPoint.getCoordinate();
            int serialID = 0;
            for (int index = 0; index < sides; index++) {
                double angle = ((double) index / (double) sides) * Math.PI * 2.0;
                double dx = Math.cos(angle) * radius;
                double dy = Math.sin(angle) * radius;

                Coordinate to = new Coordinate(center.x + dx, center.y + dy);
                Geometry los = process.getLineOfSight(center, to, observerOffset, useCurvature,
                        useRefraction, refractionFactor);
                if (los == null) {
                    continue;
                }

                int previsible = -1;
                List<Geometry> segments = new ArrayList<Geometry>();
                GeometryFactory gf = los.getFactory();
                for (int idx = 0; idx < los.getNumGeometries(); idx++) {
                    Coordinate[] coordinates = los.getCoordinates();
                    for (int i = 0; i < coordinates.length - 1; i++) {
                        int visible = (int) coordinates[i + 1].z;
                        if (i == 0) {
                            previsible = visible;
                            segments.clear();
                        }

                        LineSegment seg = new LineSegment(coordinates[i], coordinates[i + 1]);
                        Geometry lineseg = seg.toGeometry(los.getFactory());

                        if (visible == previsible) {
                            segments.add(lineseg);
                        } else {
                            LineMerger lineMerger = new LineMerger();
                            lineMerger.add(segments);
                            Geometry mergeMls = gf.createMultiLineString(GeometryFactory
                                    .toLineStringArray(lineMerger.getMergedLineStrings()));

                            String fid = featureType.getTypeName() + "." + (++serialID);
                            SimpleFeature newFeature = builder.buildFeature(fid);
                            newFeature.setDefaultGeometry(mergeMls);
                            newFeature.setAttribute(ANGLE_FIELD,
                                    Math.round(angle * (180.0 / Math.PI)));
                            newFeature.setAttribute(VALUE_FIELD, visible);
                            resultSfc.add(newFeature);

                            segments.clear();
                            previsible = visible;
                            segments.add(lineseg);
                        }
                    }

                    if (segments.size() > 0) {
                        LineMerger lineMerger = new LineMerger();
                        lineMerger.add(segments);
                        Geometry mergeMls = gf.createMultiLineString(GeometryFactory
                                .toLineStringArray(lineMerger.getMergedLineStrings()));

                        String fid = featureType.getTypeName() + "." + (++serialID);
                        SimpleFeature newFeature = builder.buildFeature(fid);
                        newFeature.setDefaultGeometry(mergeMls);
                        newFeature.setAttribute(ANGLE_FIELD, Math.round(angle * (180.0 / Math.PI)));
                        newFeature.setAttribute(VALUE_FIELD, previsible);
                        resultSfc.add(newFeature);
                    }
                }
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(RasterRadialLOSProcessFactory.RESULT.key, resultSfc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
