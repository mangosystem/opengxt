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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.util.CoordinateTranslateFilter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Offset Features SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OffsetFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(OffsetFeatureCollection.class);

    private Expression offsetX = ff.literal(0d);

    private Expression offsetY = ff.literal(0d);

    private ReferencedEnvelope offsetBounds = null;

    public OffsetFeatureCollection(SimpleFeatureCollection delegate, double offsetX, double offsetY) {
        this(delegate, ff.literal(offsetX), ff.literal(offsetY));

        offsetBounds = delegate.getBounds();
        offsetBounds.translate(offsetY, offsetY);
    }

    public OffsetFeatureCollection(SimpleFeatureCollection delegate, Expression offsetX,
            Expression offsetY) {
        super(delegate);

        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new OffsetFeatureIterator(delegate.features(), getSchema(), offsetX, offsetY);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        if (offsetBounds == null) {
            return DataUtilities.bounds(features());
        }
        return offsetBounds;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class OffsetFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private Expression offsetX = ff.literal(0d);

        private Expression offsetY = ff.literal(0d);

        private SimpleFeatureBuilder builder;

        public OffsetFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Expression offsetX, Expression offsetY) {
            this.delegate = delegate;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
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
            Double dX = offsetX.evaluate(feature, Double.class);
            Double dY = offsetY.evaluate(feature, Double.class);

            if (dX == null) {
                dX = Double.valueOf(0d);
            }

            if (dY == null) {
                dY = Double.valueOf(0d);
            }

            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;
                    if (dX > 0 || dY > 0) {
                        Geometry offseted = (Geometry) geometry.clone();
                        offseted.apply(new CoordinateTranslateFilter(dX.doubleValue(), dY
                                .doubleValue()));
                        attribute = offseted;
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
