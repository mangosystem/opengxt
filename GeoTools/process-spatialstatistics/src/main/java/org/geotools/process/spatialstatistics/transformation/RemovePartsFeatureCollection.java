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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Remove Parts SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RemovePartsFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(RemovePartsFeatureCollection.class);

    private Expression minimumArea;

    public RemovePartsFeatureCollection(SimpleFeatureCollection delegate) {
        this(delegate, ff.literal(0.0d));
    }

    public RemovePartsFeatureCollection(SimpleFeatureCollection delegate, double minimumArea) {
        this(delegate, ff.literal(minimumArea));
    }

    public RemovePartsFeatureCollection(SimpleFeatureCollection delegate, Expression minimumArea) {
        super(delegate);

        this.minimumArea = minimumArea;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new RemoveHolesFeatureIterator(delegate.features(), getSchema(), minimumArea);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class RemoveHolesFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private Expression minimumArea;

        private SimpleFeatureBuilder builder;

        public RemoveHolesFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Expression minimumArea) {
            this.delegate = delegate;
            this.minimumArea = minimumArea;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            Double minArea = minimumArea.evaluate(feature, Double.class);

            // largest part will be kept in the output while all other parts will be removed.
            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    if (minArea == null || minArea == 0.0 || Double.isNaN(minArea)) {
                        attribute = removeParts((Geometry) attribute);
                    } else {
                        attribute = removeSmallParts((Geometry) attribute, minArea);
                    }
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }

        private Geometry removeParts(Geometry polygon) {
            Class<?> geomBinding = polygon.getClass();

            if (Polygon.class.equals(geomBinding)) {
                return polygon;
            } else if (MultiPolygon.class.equals(geomBinding)) {
                Geometry largest = polygon.getGeometryN(0);
                for (int index = 1; index < polygon.getNumGeometries(); index++) {
                    Geometry part = polygon.getGeometryN(index);
                    if (part.getArea() > largest.getArea()) {
                        largest = part;
                    }
                }

                // return multipolygon
                Geometry parts = polygon.getFactory().createMultiPolygon(
                        new Polygon[] { (Polygon) largest });
                parts.setUserData(polygon.getUserData());
                return parts;
            } else {
                // other type --> return source geometry
                return polygon;
            }
        }

        private Geometry removeSmallParts(Geometry polygon, double areaTolerance) {
            Class<?> geomBinding = polygon.getClass();

            if (Polygon.class.equals(geomBinding)) {
                return polygon;
            } else if (MultiPolygon.class.equals(geomBinding)) {
                List<Polygon> remains = new ArrayList<Polygon>();

                Geometry largest = polygon.getGeometryN(0);
                for (int index = 0; index < polygon.getNumGeometries(); index++) {
                    Geometry part = polygon.getGeometryN(index);
                    double area = part.getArea();
                    if (area > largest.getArea()) {
                        largest = part;
                    }
                    if (area >= areaTolerance) {
                        remains.add((Polygon) part);
                    }
                }

                if (remains.size() == 0) {
                    remains.add((Polygon) largest);
                }

                // return multipolygon
                Geometry parts = polygon.getFactory().createMultiPolygon(
                        GeometryFactory.toPolygonArray(remains));
                parts.setUserData(polygon.getUserData());
                return parts;
            } else {
                return polygon;
            }
        }
    }
}
