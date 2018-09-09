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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

/**
 * Finds the highest or lowest points for a polygon geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RadialLOSOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RadialLOSOperation.class);

    static final String ANGLE_FIELD = "Angle";

    static final String VALUE_FIELD = "Visible";

    private SimpleFeatureType createTemplateFeatures(CoordinateReferenceSystem crs) {
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("RadialLineOfSight",
                LineString.class, crs);

        featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
        featureType = FeatureTypes.add(featureType, VALUE_FIELD, Integer.class, 5);

        return featureType;
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputCoverage, Geometry observerPoint,
            Double observerOffset, Double radius, Integer sides, Boolean useCurvature,
            Boolean useRefraction, Double refractionFactor) {
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = createTemplateFeatures(crs);

        // convert unit
        boolean isGeographic = false;
        if (crs != null) {
            CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
            if (hor instanceof GeographicCRS) {
                isGeographic = true;
            } else {
                Unit<?> unit = hor.getCoordinateSystem().getAxis(0).getUnit();
                UnitConverter converter = SI.METER.getConverterTo(unit);
                radius = converter.convert(radius);
            }
        }

        // prepare transactional feature store
        ListFeatureCollection features = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
        Coordinate center = observerPoint.getCoordinate();

        GeodeticCalculator calculator = null;

        if (isGeographic) {
            calculator = new GeodeticCalculator(crs);
            calculator.setStartingGeographicPoint(center.x, center.y); // lon-lat order
        }

        int serialID = 0;
        for (int index = 0; index < sides; index++) {
            double angle = ((double) index / (double) sides) * Math.PI * 2.0;
            double dx = Math.cos(angle) * radius;
            double dy = Math.sin(angle) * radius;
            Coordinate to = new Coordinate(center.x + dx, center.y + dy);

            if (isGeographic) {
                double azimuth = 360.0 * index / sides - 180;
                calculator.setDirection(azimuth, radius);
                Point2D dp = calculator.getDestinationGeographicPoint();
                to.setOrdinate(0, dp.getX());
                to.setOrdinate(1, dp.getY());
            }

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
                        newFeature.setAttribute(ANGLE_FIELD, Math.round(angle * (180.0 / Math.PI)));
                        newFeature.setAttribute(VALUE_FIELD, visible);
                        features.add(newFeature);

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
                    features.add(newFeature);
                }
            }
        }

        return features;
    }
}
