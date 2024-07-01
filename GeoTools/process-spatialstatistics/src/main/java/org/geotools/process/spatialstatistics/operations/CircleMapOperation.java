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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * Creates a circle map from features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircleMapOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(CircleMapOperation.class);

    private double minValue = Double.MAX_VALUE;

    private double maxValue = Double.MIN_VALUE;

    private int count = 0;

    private ReferencedEnvelope bounds = null;

    private SimpleFeatureCollection anchorFeatures;

    public CircleMapOperation() {

    }

    public SimpleFeatureCollection getAnchorFeatures() {
        return anchorFeatures;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String property,
            String sortField) throws IOException {
        return execute(features, ff.property(property), sortField);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression property,
            String sortField) throws IOException {
        SimpleFeatureType inputSchema = features.getSchema();

        SimpleFeatureType circleSchema = FeatureTypes.build(inputSchema, "circularmap",
                Polygon.class);
        SimpleFeatureType anchorSchema = FeatureTypes
                .build(inputSchema, "anchor", LineString.class);

        // calculate min & max & bounds & count
        this.preCalculate(features, property);

        // centroid and bounds
        Coordinate origin = bounds.centre();
        double radius = origin.distance(new Coordinate(bounds.getMaxX(), bounds.getMaxY()));

        double angle = 360.0 / count;
        double diff = maxValue - minValue;

        double maxRadius = getRadius(origin, radius, angle);
        double minRadius = maxRadius * 0.1;

        // build spatial index
        STRtree spatialIndex = new STRtree();
        for (int index = 0; index < count; index++) {
            double degree = 360 - (index * angle);
            Point centroid = createPoint(origin, degree, radius);

            Circle near = new Circle(index, centroid);
            spatialIndex.insert(centroid.getEnvelopeInternal(), near);
        }

        SimpleFeatureIterator featureIter = null;
        if (sortField != null && sortField.length() > 0) {
            SortBy sort = ff.sort(sortField, SortOrder.DESCENDING);
            featureIter = features.sort(sort).features();
        } else {
            featureIter = features.features();
        }

        IFeatureInserter circleWriter = getFeatureWriter(circleSchema);
        IFeatureInserter anchorWriter = getFeatureWriter(anchorSchema);
        try {
            final List<Integer> processed = new ArrayList<Integer>();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Point centroid = getMaxAreaPolygon(geometry).getInteriorPoint();

                // find nearest circle
                Circle nearest = (Circle) spatialIndex.nearestNeighbour(
                        centroid.getEnvelopeInternal(), new Circle(-1, centroid),
                        new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                Circle s1 = (Circle) item1.getItem();
                                if (processed.contains(s1.id)) {
                                    return Double.MAX_VALUE;
                                }
                                Circle s2 = (Circle) item2.getItem();
                                return s1.point.distance(s2.point);
                            }
                        });

                processed.add(nearest.id);

                // create circle & anchor
                Double value = property.evaluate(feature, Double.class);
                double adjustedRadius = ((value - minValue) / diff) * maxRadius;
                if (adjustedRadius <= minRadius) {
                    adjustedRadius = minRadius;
                }

                Geometry circle = nearest.point.buffer(adjustedRadius, 8);
                Coordinate[] coords = DistanceOp.nearestPoints(centroid, circle);
                Geometry anchor = gf.createLineString(coords);

                // write a circle feature
                SimpleFeature circleFeature = circleWriter.buildFeature();
                circleWriter.copyAttributes(feature, circleFeature, false);
                circleFeature.setDefaultGeometry(circle);
                circleWriter.write(circleFeature);

                // writer a anchor feature
                SimpleFeature anchorFeature = anchorWriter.buildFeature();
                anchorWriter.copyAttributes(feature, anchorFeature, false);
                anchorFeature.setDefaultGeometry(anchor);
                anchorWriter.write(anchorFeature);
            }
        } catch (Exception e) {
            circleWriter.rollback(e);
            anchorWriter.rollback(e);
        } finally {
            circleWriter.close();
            anchorWriter.close();
            featureIter.close();
        }

        this.anchorFeatures = anchorWriter.getFeatureCollection();

        return circleWriter.getFeatureCollection();
    }

    private void preCalculate(SimpleFeatureCollection features, Expression property) {
        SimpleFeatureType schema = features.getSchema();
        this.bounds = new ReferencedEnvelope(schema.getCoordinateReferenceSystem());
        this.count = 0;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Double value = property.evaluate(feature, Double.class);
                if (value != null) {
                    minValue = Math.min(minValue, value);
                    maxValue = Math.max(maxValue, value);
                }

                Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
                Geometry maxGeometry = getMaxAreaPolygon(sourceGeometry);

                this.bounds.expandToInclude(maxGeometry.getEnvelopeInternal());
                this.count++;
            }
        } finally {
            featureIter.close();
        }
    }

    private double getRadius(Coordinate origin, double radius, double angle) {
        Point p1 = createPoint(origin, 0, radius);
        Point p2 = createPoint(origin, angle, radius);

        return p1.distance(p2) / 2.0;
    }

    private Point createPoint(Coordinate origin, double degree, double radius) {
        double radian = Math.toRadians(degree);
        double dx = Math.cos(radian) * radius;
        double dy = Math.sin(radian) * radius;

        return gf.createPoint(new Coordinate(origin.x + dx, origin.y + dy));
    }

    private Geometry getMaxAreaPolygon(Geometry input) {
        double maxArea = Double.MIN_VALUE;
        Geometry result = input;

        for (int index = 0; index < input.getNumGeometries(); index++) {
            Geometry currentGeometry = input.getGeometryN(index);
            double currentArea = currentGeometry.getArea();
            if (currentArea > maxArea) {
                maxArea = currentArea;
                result = currentGeometry;
            }
        }
        return result;
    }

    static final class Circle {

        public Integer id;

        public Geometry point;

        public Circle(Integer id, Geometry point) {
            this.id = id;
            this.point = point;
        }
    }
}
