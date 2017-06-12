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
 * Determines the visibility, based on the elevation, of all the points in a straight line on a surface between observer and target points.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterLinearLOSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterLinearLOSProcess.class);

    static final String VALUE_FIELD = "Visible";

    public RasterLinearLOSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage,
            Geometry observerPoint, Double observerOffset, Geometry targetPoint,
            Boolean useCurvature, Boolean useRefraction, Double refractionFactor,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterLinearLOSProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterLinearLOSProcessFactory.observerPoint.key, observerPoint);
        map.put(RasterLinearLOSProcessFactory.observerOffset.key, observerOffset);
        map.put(RasterLinearLOSProcessFactory.targetPoint.key, targetPoint);
        map.put(RasterLinearLOSProcessFactory.useCurvature.key, useCurvature);
        map.put(RasterLinearLOSProcessFactory.useRefraction.key, useRefraction);
        map.put(RasterLinearLOSProcessFactory.refractionFactor.key, refractionFactor);

        Process process = new RasterLinearLOSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RasterLinearLOSProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterLinearLOSProcessFactory.inputCoverage, null);
        Geometry observerPoint = (Geometry) Params.getValue(input,
                RasterLinearLOSProcessFactory.observerPoint, null);
        Double observerOffset = (Double) Params.getValue(input,
                RasterLinearLOSProcessFactory.observerOffset,
                RasterLinearLOSProcessFactory.observerOffset.sample);
        Geometry targetPoint = (Geometry) Params.getValue(input,
                RasterLinearLOSProcessFactory.targetPoint, null);
        Boolean useCurvature = (Boolean) Params.getValue(input,
                RasterLinearLOSProcessFactory.useCurvature,
                RasterLinearLOSProcessFactory.useCurvature.sample);
        Boolean useRefraction = (Boolean) Params.getValue(input,
                RasterLinearLOSProcessFactory.useRefraction,
                RasterLinearLOSProcessFactory.useRefraction.sample);
        Double refractionFactor = (Double) Params.getValue(input,
                RasterLinearLOSProcessFactory.refractionFactor,
                RasterLinearLOSProcessFactory.refractionFactor.sample);

        if (inputCoverage == null || observerPoint == null || targetPoint == null) {
            throw new NullPointerException(
                    "inputCoverage, observerPoint, targetPoint parameters required");
        }

        if (observerOffset < 0) {
            throw new NullPointerException("observerOffset parameter must be a positive value");
        }

        // start process
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        observerPoint = transformGeometry(observerPoint, crs);
        targetPoint = transformGeometry(targetPoint, crs);

        LineSegment segment = new LineSegment(observerPoint.getCoordinate(),
                targetPoint.getCoordinate());
        LineString userLine = segment.toGeometry(observerPoint.getFactory());

        RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
        LineString los = process.getLineOfSight(userLine, observerOffset, useCurvature,
                useRefraction, refractionFactor);

        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("LinearLineOfSight",
                LineString.class, crs);
        featureType = FeatureTypes.add(featureType, VALUE_FIELD, Integer.class, 5);

        // prepare transactional feature store
        ListFeatureCollection resultSfc = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        if (los != null) {
            int serialID = 0;
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
                    newFeature.setAttribute(VALUE_FIELD, previsible);
                    resultSfc.add(newFeature);
                }
            }
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterLinearLOSProcessFactory.RESULT.key, resultSfc);
        return resultMap;
    }
}
