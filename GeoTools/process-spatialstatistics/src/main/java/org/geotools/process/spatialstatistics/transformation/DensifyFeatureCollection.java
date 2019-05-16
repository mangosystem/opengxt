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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Densify SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DensifyFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(DensifyFeatureCollection.class);

    private Expression tolerance;

    public DensifyFeatureCollection(SimpleFeatureCollection delegate, double tolerance) {
        this(delegate, ff.literal(tolerance));
    }

    public DensifyFeatureCollection(SimpleFeatureCollection delegate, Expression tolerance) {
        super(delegate);

        this.tolerance = tolerance;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new DensifyFeatureIterator(delegate.features(), getSchema(), tolerance);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class DensifyFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private Expression tolerance;

        private SimpleFeatureBuilder builder;

        public DensifyFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Expression tolerance) {
            this.delegate = delegate;
            this.tolerance = tolerance;
            builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            Double distanceTolerance = tolerance.evaluate(feature, Double.class);

            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    if (distanceTolerance != null && distanceTolerance > 0) {
                        Geometry geometry = (Geometry) attribute;
                        geometry = Densifier.densify(geometry, distanceTolerance);
                        if (geometry != null && !geometry.isEmpty()) {
                            geometry.setUserData(((Geometry) attribute).getUserData());
                            attribute = geometry;
                        }
                    }
                }
                builder.add(attribute);
            }

            SimpleFeature nextFeature = builder.buildFeature(feature.getID());
            builder.reset();

            return nextFeature;
        }
    }
}
