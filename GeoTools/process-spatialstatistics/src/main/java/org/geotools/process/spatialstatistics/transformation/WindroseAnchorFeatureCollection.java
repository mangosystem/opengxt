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

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Windrose Anchor SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WindroseAnchorFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(WindroseAnchorFeatureCollection.class);

    static final int SEG = 32;

    static String[] NORTH = { "E", "ENE", "NE", "NNE", "N", "NNW", "NW", "WNW", "W", "WSW", "SW",
            "SSW", "S", "SSE", "SE", "ESE" };

    static String[] FIELDS = { "distance", "direction" };

    private Point center;

    private double radius;

    private SimpleFeatureType schema;

    public WindroseAnchorFeatureCollection(SimpleFeatureCollection delegate, Point center) {
        super(delegate);

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
        schema = FeatureTypes.getDefaultType("windrose_anchor", LineString.class, crs);
        schema = FeatureTypes.add(schema, FIELDS[0], Double.class, 38);
        schema = FeatureTypes.add(schema, FIELDS[1], String.class, 6);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new WindroseAnchorFeatureIterator(getSchema(), center, radius);
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
        return 21;
    }

    static class WindroseAnchorFeatureIterator implements SimpleFeatureIterator {

        private Point center;

        private double radius;

        private SimpleFeatureIterator iter;

        public WindroseAnchorFeatureIterator(SimpleFeatureType schema, Point center, double radius) {
            this.center = center;
            this.radius = radius;

            this.init(new SimpleFeatureBuilder(schema));
        }

        private void init(SimpleFeatureBuilder builder) {
            ListFeatureCollection result = new ListFeatureCollection(builder.getFeatureType());

            int featureID = 1;
            // create circle
            double radius_step = radius / 5;
            for (int index = 0; index < 5; index++) {
                double buffer_radius = radius_step * (index + 1);
                SimpleFeature feature = builder.buildFeature(Integer.toString(featureID++));
                feature.setDefaultGeometry(center.buffer(buffer_radius, SEG).getBoundary());

                feature.setAttribute(FIELDS[0], buffer_radius);
                result.add(feature);
            }

            // create direction
            for (int index = 0; index < 16; index++) {
                double degree = 22.5 * index;
                SimpleFeature feature = builder.buildFeature(Integer.toString(featureID++));
                feature.setDefaultGeometry(createLine(center, Math.toRadians(degree), radius));

                feature.setAttribute(FIELDS[1], NORTH[index]);
                result.add(feature);
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

        private Coordinate createPoint(Coordinate centroid, double radian, double radius) {
            double dx = Math.cos(radian) * radius;
            double dy = Math.sin(radian) * radius;
            return new Coordinate(centroid.x + dx, centroid.y + dy);
        }

        private Geometry createLine(Point centroid, double radian, double radius) {
            Coordinate[] coordinates = new Coordinate[2];
            coordinates[0] = centroid.getCoordinate();
            coordinates[1] = createPoint(coordinates[0], radian, radius);
            return centroid.getFactory().createLineString(coordinates);
        }
    }
}