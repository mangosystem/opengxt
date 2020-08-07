/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.process.spatialstatistics.util.GeometryDimensions;
import org.geotools.process.spatialstatistics.util.GeometryDimensions.DimensionType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Force Dimension(2, 3, 4) SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ForceDimensionFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ForceDimensionFeatureCollection.class);

    private DimensionType dimension = DimensionType.XY;

    private Expression zField = null;

    private Expression mField = null;

    public ForceDimensionFeatureCollection(SimpleFeatureCollection delegate,
            DimensionType dimension) {
        this(delegate, dimension, null, null);
    }

    public ForceDimensionFeatureCollection(SimpleFeatureCollection delegate,
            DimensionType dimension, Expression zField) {
        this(delegate, dimension, zField, null);
    }

    public ForceDimensionFeatureCollection(SimpleFeatureCollection delegate,
            DimensionType dimension, Expression zField, Expression mField) {
        super(delegate);

        this.dimension = dimension;
        this.zField = zField;
        this.mField = mField;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ForceDimensionFeatureIterator(delegate.features(), getSchema(), dimension,
                zField, mField);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class ForceDimensionFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private DimensionType dimension = DimensionType.XY;

        private Expression zField = null;

        private Expression mField = null;

        private SimpleFeatureBuilder builder;

        public ForceDimensionFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, DimensionType dimension, Expression zField,
                Expression mField) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(schema);
            this.dimension = dimension;
            this.zField = zField;
            this.mField = mField;
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;

                    Double zValue = null;
                    Double mValue = null;

                    if (zField != null && (dimension == DimensionType.XYZ
                            || dimension == DimensionType.XYZM)) {
                        zValue = zField.evaluate(feature, Double.class);
                    }

                    if (mField != null && (dimension == DimensionType.XYM
                            || dimension == DimensionType.XYZM)) {
                        mValue = mField.evaluate(feature, Double.class);
                    }

                    // force dimension
                    attribute = GeometryDimensions.force(geometry, dimension, zValue, mValue);
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }
    }
}
