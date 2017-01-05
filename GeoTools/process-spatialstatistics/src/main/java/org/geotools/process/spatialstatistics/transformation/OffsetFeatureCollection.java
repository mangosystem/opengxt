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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
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

    private double offsetX = 0d;

    private double offsetY = 0d;

    public OffsetFeatureCollection(SimpleFeatureCollection delegate, double offsetX, double offsetY) {
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
        ReferencedEnvelope bounds = delegate.getBounds();
        bounds.translate(offsetY, offsetY);
        return bounds;
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

        private double offsetX = 0d;

        private double offsetY = 0d;

        private SimpleFeatureBuilder builder;

        public OffsetFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                double offsetX, double offsetY) {
            this.delegate = delegate;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
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

            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;
                    if (offsetX > 0 || offsetY > 0) {
                        Geometry offseted = (Geometry) geometry.clone();
                        offseted.apply(new OffsetOrdinateFilter(offsetX, offsetY));
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

    static final class OffsetOrdinateFilter implements CoordinateSequenceFilter {
        private double offsetX;

        private double offsetY;

        public OffsetOrdinateFilter(double offsetX, double offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        public void filter(CoordinateSequence seq, int i) {
            seq.setOrdinate(i, 0, seq.getOrdinate(i, 0) + offsetX);
            seq.setOrdinate(i, 1, seq.getOrdinate(i, 1) + offsetY);
        }

        public boolean isDone() {
            return false;
        }

        public boolean isGeometryChanged() {
            return true;
        }
    }
}
