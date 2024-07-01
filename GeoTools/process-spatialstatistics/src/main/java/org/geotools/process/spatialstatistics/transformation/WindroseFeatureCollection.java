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
package org.geotools.process.spatialstatistics.transformation;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Windrose SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WindroseFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(WindroseFeatureCollection.class);

    static final int SEG = 32;

    static String[] FIELDS = { "uid", "count", "min", "max", "sum", "mean", "std_dev", "var" };

    final static GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    private Expression weightExp;

    private Point center;

    private double radius;

    private SimpleFeatureType schema;

    public WindroseFeatureCollection(SimpleFeatureCollection delegate, String weightField,
            Point center) {
        super(delegate);

        if (weightField == null || weightField.isEmpty()) {
            this.weightExp = ff.literal(1);
        } else {
            this.weightExp = ff.property(weightField);
        }

        ReferencedEnvelope bounds = delegate.getBounds();
        double xF = Math.pow(bounds.getMaxX() - bounds.getMinX(), 2.0);
        double yF = Math.pow(bounds.getMaxY() - bounds.getMinY(), 2.0);
        this.radius = (Math.pow(xF + yF, 0.5)) / 1.98;

        this.center = center;
        if (center == null) {
            this.center = new GeometryFactory().createPoint(bounds.centre());
        }

        // create schema
        CoordinateReferenceSystem crs = delegate.getSchema().getCoordinateReferenceSystem();
        schema = FeatureTypes.getDefaultType("windrose", Polygon.class, crs);
        schema = FeatureTypes.add(schema, FIELDS[0], Integer.class, 38);
        schema = FeatureTypes.add(schema, FIELDS[1], Integer.class, 38);
        for (int i = 2; i < FIELDS.length; i++) {
            schema = FeatureTypes.add(schema, FIELDS[i], Double.class, 38);
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new WindroseFeatureIterator(delegate, getSchema(), weightExp, center, radius);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    @Override
    public int size() {
        return SEG;
    }

    static class WindroseFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureCollection delegate;

        private Expression weightExp;

        private Coordinate center;

        private double radius;

        private SimpleFeatureIterator iter;

        public WindroseFeatureIterator(SimpleFeatureCollection delegate, SimpleFeatureType schema,
                Expression weightExp, Point center, double radius) {
            // use SpatialIndexFeatureCollection
            this.delegate = DataUtils.toSpatialIndexFeatureCollection(delegate);

            this.weightExp = weightExp;
            this.center = center.getCoordinate();
            this.radius = radius;

            this.init(new SimpleFeatureBuilder(schema));
        }

        private void init(SimpleFeatureBuilder builder) {
            String the_geom = delegate.getSchema().getGeometryDescriptor().getLocalName();
            String typeName = builder.getFeatureType().getTypeName();

            ListFeatureCollection result = new ListFeatureCollection(builder.getFeatureType());

            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;

            double stepAngle = 360.0 / SEG;
            double halfStep = stepAngle / 2.0;

            // pre process
            for (int index = 0; index < SEG; index++) {
                double startDeg = (index * stepAngle) - halfStep;
                double endDeg = ((index + 1) * stepAngle) - halfStep;
                Geometry cell = createCell(center, startDeg, endDeg, radius);

                StatisticsVisitor visitor = new StatisticsVisitor(weightExp, null);
                visitor.visit(delegate.subCollection(getIntersectsFilter(the_geom, cell)));
                StatisticsVisitorResult ret = visitor.getResult();

                // { "uid", "count", "min", "max", "sum", "mean", "std_dev", "var" };
                SimpleFeature feature = builder.buildFeature(buildID(typeName, index));
                feature.setDefaultGeometry(cell);

                feature.setAttribute(FIELDS[0], index);
                feature.setAttribute(FIELDS[1], ret.getCount());
                if (ret.getCount() == 0) {
                    feature.setAttribute(FIELDS[2], 0.0);
                    feature.setAttribute(FIELDS[3], 0.0);
                    feature.setAttribute(FIELDS[4], 0.0);
                    feature.setAttribute(FIELDS[5], 0.0);
                    feature.setAttribute(FIELDS[6], 0.0);
                    feature.setAttribute(FIELDS[7], 0.0);
                } else {
                    feature.setAttribute(FIELDS[2], ret.getMinimum());
                    feature.setAttribute(FIELDS[3], ret.getMaximum());
                    feature.setAttribute(FIELDS[4], ret.getSum());
                    feature.setAttribute(FIELDS[5], ret.getMean());
                    feature.setAttribute(FIELDS[6], ret.getStandardDeviation());
                    feature.setAttribute(FIELDS[7], ret.getVariance());
                }

                minValue = Math.min(minValue, ret.getSum());
                maxValue = Math.max(maxValue, ret.getSum());

                result.add(feature);
            }

            // post process
            SimpleFeatureIterator featureIter = result.features();
            try {
                double diff = maxValue - minValue;
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    int index = (Integer) feature.getAttribute(FIELDS[0]);
                    double value = (Double) feature.getAttribute(FIELDS[4]);

                    double adjustedRadius = ((value - minValue) / diff) * radius;
                    if (adjustedRadius == 0) {
                        adjustedRadius = radius * 0.001;
                    }

                    double startDeg = (index * stepAngle) - halfStep;
                    double endDeg = ((index + 1) * stepAngle) - halfStep;
                    feature.setDefaultGeometry(
                            createCell(center, startDeg, endDeg, adjustedRadius));
                }
            } finally {
                featureIter.close();
            }

            this.iter = result.features();
        }

        public void close() {
            this.iter.close();
        }

        public boolean hasNext() {
            return this.iter.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            return this.iter.next();
        }

        private Geometry createCell(Coordinate centroid, double from_deg, double to_deg,
                double radius) {
            CoordinateList coordinates = new CoordinateList();
            coordinates.add(centroid, false);

            double step = Math.abs(to_deg - from_deg) / SEG;
            for (int i = 0; i <= SEG; i++) {
                double radian = Math.toRadians(from_deg + (i * step));
                coordinates.add(createPoint(centroid, radian, radius), false);
            }
            coordinates.add(centroid, false);
            return gf.createPolygon(coordinates.toCoordinateArray());
        }

        private Coordinate createPoint(Coordinate centroid, double radian, double radius) {
            double dx = Math.cos(radian) * radius;
            double dy = Math.sin(radian) * radius;
            return new Coordinate(centroid.x + dx, centroid.y + dy);
        }
    }
}