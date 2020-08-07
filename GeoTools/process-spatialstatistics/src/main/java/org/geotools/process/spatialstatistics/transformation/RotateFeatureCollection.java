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
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Rotate SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RotateFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(RotateFeatureCollection.class);

    private Expression angleInDegree;

    private Coordinate anchor = null;

    private boolean anchorAsEachFeature = false;

    public boolean isAnchorAsEachFeature() {
        return anchorAsEachFeature;
    }

    public void setAnchorAsEachFeature(boolean anchorAsEachFeature) {
        this.anchorAsEachFeature = anchorAsEachFeature;
    }

    public RotateFeatureCollection(SimpleFeatureCollection delegate, double angleInDegree) {
        this(delegate, (Coordinate) null, angleInDegree);
    }

    public RotateFeatureCollection(SimpleFeatureCollection delegate, Coordinate anchor,
            double angleInDegree) {
        this(delegate, anchor, ff.literal(angleInDegree));
    }

    public RotateFeatureCollection(SimpleFeatureCollection delegate, Coordinate anchor,
            Expression angleInDegree) {
        super(delegate);

        this.angleInDegree = angleInDegree;

        if (anchor == null && !anchorAsEachFeature) {
            this.anchor = delegate.getBounds().centre();
        } else {
            this.anchor = anchor;
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new RotateFeatureIterator(delegate.features(), getSchema(), anchor, angleInDegree);
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

    static class RotateFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private SimpleFeatureBuilder builder;

        private Expression angleInDegree;

        private Coordinate anchor;

        public RotateFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Coordinate anchor, Expression angleInDegree) {
            this.delegate = delegate;

            this.anchor = anchor;
            this.angleInDegree = angleInDegree;
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
                    Double theta = angleInDegree.evaluate(sourceFeature, Double.class);

                    if (theta != null && !theta.isNaN() && !theta.isInfinite()) {
                        // AffineTransformation: Positive angles correspond to a rotation in the counter-clockwise direction.
                        double rad = -Math.toRadians(theta);

                        AffineTransformation trans;
                        if (anchor == null) {
                            Coordinate tx = geometry.getCentroid().getCoordinate();
                            trans = AffineTransformation.rotationInstance(rad, tx.x, tx.y);
                        } else {
                            trans = AffineTransformation.rotationInstance(rad, anchor.x, anchor.y);
                        }
                        attribute = trans.transform(geometry);
                    }
                }
                builder.add(attribute);
            }

            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());
            builder.reset();

            return nextFeature;
        }
    }
}