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
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Create points along lines.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsAlongLinesFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(PointsAlongLinesFeatureCollection.class);

    private SimpleFeatureType schema;

    private Expression distance;

    public PointsAlongLinesFeatureCollection(SimpleFeatureCollection delegate, Expression distance) {
        super(delegate);

        this.distance = distance;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Point.class);
        this.schema = FeatureTypes.add(this.schema, "se_order", Integer.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new PointsAlongLinesFeatureIterator(delegate.features(), getSchema(), distance);
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
    public int size() {
        return DataUtilities.count(features());
    }

    static class PointsAlongLinesFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private List<Geometry> points;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private Expression distance;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        private String typeName;

        public PointsAlongLinesFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression distance) {
            this.delegate = delegate;

            this.index = 0;
            this.builder = new SimpleFeatureBuilder(schema);
            this.distance = distance;
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((nextFeature == null && delegate.hasNext())
                    || (nextFeature == null && !delegate.hasNext() && index > 0)) {
                if (index == 0) {
                    origFeature = delegate.next();
                    Double interval = distance.evaluate(origFeature, Double.class);
                    Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                    points = extractPoints(geometry, interval);
                }

                // create feature
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = points.get(index);
                    }
                    builder.add(attribute);
                }
                builder.add(new Integer(index));
                nextFeature = builder.buildFeature(buildID(typeName, ++featureID));
                builder.reset();
                index++;

                if (index >= points.size()) {
                    index = 0;
                    origFeature = null;
                }
            }
            return nextFeature != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = nextFeature;
            nextFeature = null;
            return result;
        }

        private List<Geometry> extractPoints(Geometry geometry, double interval) {
            List<Geometry> points = new ArrayList<Geometry>();

            Geometry multiPart = geometry;
            Class<?> binding = geometry.getClass();
            if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
                multiPart = geometry.getBoundary();
            }

            for (int index = 0; index < multiPart.getNumGeometries(); index++) {
                LineString part = (LineString) multiPart.getGeometryN(index);
                points.add(part.getStartPoint()); // first one

                LengthIndexedLine lil = new LengthIndexedLine(part);
                int count = (int) Math.ceil(part.getLength() / interval);

                double startIndex = 0;
                for (int i = 0; i < count; i++) {
                    LineString segment = (LineString) lil.extractLine(startIndex, startIndex
                            + interval);
                    points.add(segment.getEndPoint());
                    startIndex += interval;
                }
            }

            return points;
        }
    }
}
