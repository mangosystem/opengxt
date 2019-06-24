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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Create equal distance polygons along lines.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolygonsAlongLinesFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(PolygonsAlongLinesFeatureCollection.class);

    private SimpleFeatureType schema;

    private Expression distance;

    private Expression width;

    private double mergeFactor = 0d; // 0.0 ~ 1.0

    public PolygonsAlongLinesFeatureCollection(SimpleFeatureCollection delegate,
            Expression distance, Expression width) {
        this(delegate, distance, width, 0d);
    }

    public PolygonsAlongLinesFeatureCollection(SimpleFeatureCollection delegate,
            Expression distance, Expression width, double mergeFactor) {
        super(delegate);

        this.distance = distance;
        this.width = width;
        this.mergeFactor = mergeFactor;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
        this.schema = FeatureTypes.add(this.schema, "se_order", Integer.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new RectangleAlongLinesFeatureIterator(delegate.features(), getSchema(), distance,
                width, mergeFactor);
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

    static class RectangleAlongLinesFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private List<Geometry> rectangles;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private Expression distance;

        private Expression width;

        private double mergeFactor = 0d; // 0.0 ~ 1.0

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        private String typeName;

        public RectangleAlongLinesFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression distance, Expression width, double mergeFactor) {
            this.delegate = delegate;

            this.index = 0;
            this.builder = new SimpleFeatureBuilder(schema);
            this.distance = distance;
            this.mergeFactor = mergeFactor < 0d ? 0d : mergeFactor;
            this.width = width;
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
                    Double radius = width.evaluate(origFeature, Double.class);
                    if (interval == null || interval.isInfinite() || interval.isNaN()
                            || interval == 0d || radius == null || radius.isInfinite()
                            || radius.isNaN() || radius == 0d) {
                        continue;
                    }

                    Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                    rectangles = processGeometry(geometry, interval, radius);
                }

                // create feature
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = rectangles.get(index);
                    }
                    builder.add(attribute);
                }

                builder.add(new Integer(index));
                nextFeature = builder.buildFeature(buildID(typeName, ++featureID));
                builder.reset();
                index++;

                if (index >= rectangles.size()) {
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

        private List<Geometry> processGeometry(Geometry multiLineString, double distance,
                double width) {
            List<Geometry> rects = new ArrayList<Geometry>();

            Geometry multiPart = multiLineString;
            Class<?> binding = multiLineString.getClass();
            if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
                multiPart = multiLineString.getBoundary();
            }

            for (int idxGeom = 0; idxGeom < multiPart.getNumGeometries(); idxGeom++) {
                LineString line = (LineString) multiPart.getGeometryN(idxGeom);
                if (line.getLength() <= distance) {
                    rects.add(BufferOp.bufferOp(line, width, 24, BufferParameters.CAP_FLAT));
                    break;
                }

                LengthIndexedLine lil = new LengthIndexedLine(line);

                int count = (int) Math.ceil(line.getLength() / distance);
                double start = 0;
                for (int i = 1; i <= count; i++) {
                    LineString seg = (LineString) lil.extractLine(start, start + distance);

                    Geometry rect = BufferOp.bufferOp(seg, width, 24, BufferParameters.CAP_FLAT);
                    if (i == count) {
                        if (seg.getLength() <= (distance * mergeFactor)) {
                            seg = (LineString) lil.extractLine(start - distance, start + distance);
                            rect = BufferOp.bufferOp(seg, width, 24, BufferParameters.CAP_FLAT);
                            rects.set(rects.size() - 1, rect);
                        } else {
                            rects.add(rect);
                        }
                    } else {
                        rects.add(rect);
                    }

                    start += distance;
                }
            }

            return rects;
        }
    }
}
