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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.StatisticsStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a flow map features using an origin-destination line features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FlowMapFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(FlowMapFeatureCollection.class);

    private Expression odValue;

    private Expression doValue;

    private Double maxSize = null;

    private double[] minMax = new double[4];

    private SimpleFeatureType schema;

    public FlowMapFeatureCollection(SimpleFeatureCollection delegate, Expression odValue,
            Expression doValue, Double maxSize) {
        super(delegate);

        this.odValue = odValue;
        this.doValue = doValue;
        this.maxSize = maxSize;

        this.calculateMinMax();

        if (this.maxSize == null || this.maxSize == 0) {
            ReferencedEnvelope bbox = delegate.getBounds();
            this.maxSize = Math.min(bbox.getWidth(), bbox.getHeight()) / 20;
            LOGGER.log(Level.WARNING, "The default maxSize is " + this.maxSize);
        }

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
    }

    private void calculateMinMax() {
        StatisticsStrategy odVisitor = new DoubleStrategy();
        StatisticsStrategy doVisitor = new DoubleStrategy();
        SimpleFeatureIterator featureIter = delegate.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Double value = odValue.evaluate(feature, Double.class);
                if (value != null) {
                    odVisitor.add(value);
                }

                if (doValue != null) {
                    value = doValue.evaluate(feature, Double.class);
                    if (value != null) {
                        doVisitor.add(value);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        StatisticsVisitorResult ret = odVisitor.getResult();
        this.minMax[0] = ret.getMinimum();
        this.minMax[1] = ret.getMaximum();

        ret = doVisitor.getResult();
        this.minMax[2] = ret.getMinimum();
        this.minMax[3] = ret.getMaximum();
    }

    @Override
    public SimpleFeatureIterator features() {
        return new BufferExpressionFeatureIterator(delegate.features(), getSchema(), odValue,
                doValue, minMax, maxSize);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public int size() {
        if (doValue == null) {
            return delegate.size();
        } else {
            return delegate.size() * 2;
        }
    }

    static class BufferExpressionFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private Expression odValue;

        private Expression doValue;

        private boolean bothSide = false;

        private double[] minMax = new double[4];

        private Double maxSize = null;

        private int count = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next = null;

        private SimpleFeature source = null;

        ArrowBuilder arrowBuilder = new ArrowBuilder();

        public BufferExpressionFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression odValue, Expression doValue, double[] minMax,
                Double maxSize) {
            this.delegate = delegate;

            this.odValue = odValue;
            this.doValue = doValue;
            this.bothSide = doValue == null;
            this.minMax = minMax;
            this.maxSize = maxSize;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((next == null && delegate.hasNext())
                    || (next == null && !delegate.hasNext() && index > 0)) {
                if (index == 0) {
                    source = delegate.next();
                }

                next = builder.buildFeature(Integer.toString(++count));
                next.setAttributes(source.getAttributes());
                Geometry line = (Geometry) source.getDefaultGeometry();

                double transValue = 0;
                if (index == 0) {
                    Double value = odValue.evaluate(source, Double.class);
                    if (value == null) {
                        value = minMax[0]; // minimum
                    }

                    transValue = (value - minMax[0]) / (minMax[1] - minMax[0]);
                    index = bothSide ? 0 : 1;
                } else {
                    // flip line
                    line = line.reverse();
                    Double value = doValue.evaluate(source, Double.class);
                    if (value == null) {
                        value = minMax[2]; // minimum
                    }

                    transValue = (value - minMax[2]) / (minMax[3] - minMax[2]);
                    index = 0;
                }

                // create flow arrow
                Geometry arrow = arrowBuilder.createArraw(line, transValue, maxSize, bothSide);
                next.setDefaultGeometry(arrow);
                builder.reset();
            }
            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = next;
            next = null;
            return result;
        }
    }

    /**
     * Builds flow arrows from LineString geometry
     */
    static class ArrowBuilder {
        static final double OFFSET = 10;

        public Geometry createArraw(Geometry geometry, double transValue, double maxRadius,
                boolean bothSide) {
            GeometryFactory gf = geometry.getFactory();

            LineString lineString = (LineString) geometry.getGeometryN(0);
            Coordinate from = lineString.getStartPoint().getCoordinate();
            Coordinate to = lineString.getEndPoint().getCoordinate();
            double angle = toDeg(Math.atan2(to.y - from.y, to.x - from.x)) - 180;

            double radius = transValue * maxRadius;

            List<Coordinate> coords = new ArrayList<Coordinate>();
            coords.add(from);

            if (bothSide) {
                // right side
                coords.add(createCoord(to, toRad(angle + OFFSET), radius));
                coords.add(createCoord(to, toRad(angle + (OFFSET * 2)), radius));
            }

            // center
            coords.add(to);

            // left side
            coords.add(createCoord(to, toRad(angle - (OFFSET * 2)), radius));
            coords.add(createCoord(to, toRad(angle - OFFSET), radius));

            // close rings
            coords.add(coords.get(0));

            // create polygon
            Coordinate[] ring = coords.toArray(new Coordinate[coords.size()]);
            return gf.createPolygon(gf.createLinearRing(ring), null);
        }

        private double toDeg(double radians) {
            return radians * (180.0 / Math.PI);
        }

        private double toRad(double degree) {
            return Math.PI / 180.0 * degree;
        }

        private Coordinate createCoord(Coordinate source, double radian, double radius) {
            double dx = Math.cos(radian) * radius;
            double dy = Math.sin(radian) * radius;
            return new Coordinate(source.x + dx, source.y + dy);
        }
    }
}