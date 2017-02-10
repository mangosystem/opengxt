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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Remove Holes SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RemoveHolesFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(RemoveHolesFeatureCollection.class);

    private Expression minimumArea;

    public RemoveHolesFeatureCollection(SimpleFeatureCollection delegate) {
        this(delegate, ff.literal(0.0d));
    }

    public RemoveHolesFeatureCollection(SimpleFeatureCollection delegate, double minimumArea) {
        this(delegate, ff.literal(minimumArea));
    }

    public RemoveHolesFeatureCollection(SimpleFeatureCollection delegate, Expression minimumArea) {
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

            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    if (minArea == null || minArea == 0.0 || Double.isNaN(minArea)) {
                        attribute = removeHoles((Geometry) attribute);
                    } else {
                        attribute = removeSmallHoles((Geometry) attribute, minArea);
                    }
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }

        private Geometry removeHoles(Geometry inputPolygon) {
            Class<?> geomBinding = inputPolygon.getClass();

            Geometry finalGeom = inputPolygon;
            if (Polygon.class.equals(geomBinding)) {
                finalGeom = removeHoles((Polygon) inputPolygon);
            } else if (MultiPolygon.class.equals(geomBinding)) {
                List<Polygon> polygons = new ArrayList<Polygon>();
                for (int index = 0; index < inputPolygon.getNumGeometries(); index++) {
                    Polygon polygon = (Polygon) inputPolygon.getGeometryN(index);
                    polygons.add((Polygon) removeHoles(polygon));
                }

                finalGeom = inputPolygon.getFactory().createMultiPolygon(
                        GeometryFactory.toPolygonArray(polygons));
            }
            finalGeom.setUserData(inputPolygon.getUserData());
            return finalGeom;
        }

        private Geometry removeHoles(Polygon polygon) {
            GeometryFactory factory = polygon.getFactory();
            LineString exteriorRing = polygon.getExteriorRing();
            Geometry finalGeom = factory.createPolygon((LinearRing) exteriorRing, null);
            finalGeom.setUserData(polygon.getUserData());
            return finalGeom;
        }

        private Geometry removeSmallHoles(Geometry inputPolygon, double areaTolerance) {
            Class<?> geomBinding = inputPolygon.getClass();

            Geometry finalGeom = inputPolygon;
            if (Polygon.class.equals(geomBinding)) {
                finalGeom = removeSmallHoles((Polygon) inputPolygon, areaTolerance);
            } else if (MultiPolygon.class.equals(geomBinding)) {
                List<Polygon> polygons = new ArrayList<Polygon>();
                for (int index = 0; index < inputPolygon.getNumGeometries(); index++) {
                    Polygon polygon = (Polygon) inputPolygon.getGeometryN(index);
                    polygons.add((Polygon) removeSmallHoles(polygon, areaTolerance));
                }

                finalGeom = inputPolygon.getFactory().createMultiPolygon(
                        GeometryFactory.toPolygonArray(polygons));
            }

            finalGeom.setUserData(inputPolygon.getUserData());
            return finalGeom;
        }

        private Geometry removeSmallHoles(Polygon polygon, double areaTolerance) {
            GeometryFactory factory = polygon.getFactory();
            LineString exteriorRing = polygon.getExteriorRing();

            // check interior rings
            List<LinearRing> interiorRingList = new ArrayList<LinearRing>();
            for (int index = 0; index < polygon.getNumInteriorRing(); index++) {
                LineString interiorRing = polygon.getInteriorRingN(index);
                if (interiorRing.isRing()) {
                    if (Math.abs(interiorRing.getArea()) >= areaTolerance) {
                        interiorRingList.add((LinearRing) interiorRing);
                    }
                }
            }

            LinearRing[] holes = null;
            if (interiorRingList.size() > 0) {
                holes = GeometryFactory.toLinearRingArray(interiorRingList);
            }

            Geometry finalGeom = factory.createPolygon((LinearRing) exteriorRing, holes);
            finalGeom.setUserData(polygon.getUserData());
            return finalGeom;
        }
    }
}
