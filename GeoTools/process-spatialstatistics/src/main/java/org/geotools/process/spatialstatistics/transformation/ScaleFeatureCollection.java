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
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Scale SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ScaleFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ScaleFeatureCollection.class);

    private Expression scaleX = ff.literal(1d);

    private Expression scaleY = ff.literal(1d);

    private Coordinate anchor = null;

    private boolean anchorAsEachFeature = false;

    public boolean isAnchorAsEachFeature() {
        return anchorAsEachFeature;
    }

    public void setAnchorAsEachFeature(boolean anchorAsEachFeature) {
        this.anchorAsEachFeature = anchorAsEachFeature;
    }

    public ScaleFeatureCollection(SimpleFeatureCollection delegate, double scaleX, double scaleY) {
        this(delegate, scaleX, scaleY, null);
    }

    public ScaleFeatureCollection(SimpleFeatureCollection delegate, double scaleX, double scaleY,
            Coordinate anchor) {
        this(delegate, ff.literal(scaleX), ff.literal(scaleY), anchor);
    }

    public ScaleFeatureCollection(SimpleFeatureCollection delegate, Expression scaleX,
            Expression scaleY, Coordinate anchor) {
        super(delegate);

        this.scaleX = scaleX;
        this.scaleY = scaleY;

        if (anchor == null && !anchorAsEachFeature) {
            this.anchor = delegate.getBounds().centre();
        } else {
            this.anchor = anchor;
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ScaleFeatureIterator(delegate.features(), getSchema(), scaleX, scaleY, anchor);
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
        return DataUtilities.bounds(features());
    }

    static class ScaleFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private SimpleFeatureBuilder builder;

        private Expression scaleX = ff.literal(1d);

        private Expression scaleY = ff.literal(1d);

        private Coordinate anchor;

        public ScaleFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Expression scaleX, Expression scaleY, Coordinate anchor) {
            this.delegate = delegate;

            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.anchor = anchor;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();

            for (Object attribute : sourceFeature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;

                    Double sx = scaleX.evaluate(sourceFeature, Double.class);
                    Double sy = scaleY.evaluate(sourceFeature, Double.class);

                    // default scale = 1
                    sx = sx == null ? 1.0d : sx;
                    sy = sy == null ? 1.0d : sy;

                    AffineTransformation trans;
                    if (anchor == null) {
                        Coordinate tx = geometry.getCentroid().getCoordinate();
                        trans = AffineTransformation.scaleInstance(sx, sy, tx.x, tx.y);
                    } else {
                        trans = AffineTransformation.scaleInstance(sx, sy, anchor.x, anchor.y);
                    }
                    attribute = trans.transform(geometry);
                }
                builder.add(attribute);
            }

            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());
            builder.reset();

            return nextFeature;
        }
    }
}