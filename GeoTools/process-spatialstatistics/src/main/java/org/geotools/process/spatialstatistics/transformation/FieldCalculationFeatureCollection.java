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

import java.sql.Timestamp;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;

/**
 * FieldCalculation SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FieldCalculationFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(FieldCalculationFeatureCollection.class);

    private String fieldName;

    private Expression expression;

    private Class<?> fieldBinding = null;

    private boolean isGeometry = false;

    private SimpleFeatureType schema;

    public FieldCalculationFeatureCollection(SimpleFeatureCollection delegate,
            Expression expression, String fieldName) {
        super(delegate);

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.fieldName = FeatureTypes.validateProperty(delegate.getSchema(), fieldName);
        this.expression = expression;

        // test value type
        SimpleFeatureIterator featureIter = delegate.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Object value = this.expression.evaluate(feature);
                if (value != null) {
                    fieldBinding = value.getClass();
                    break;
                }
            }
        } finally {
            featureIter.close();
        }

        if (fieldBinding == null) {
            fieldBinding = String.class;
        }

        if (fieldBinding.isAssignableFrom(String.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, String.class, 150);
        } else if (fieldBinding.isAssignableFrom(Short.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Integer.class, 38);
        } else if (fieldBinding.isAssignableFrom(Integer.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Integer.class, 38);
        } else if (fieldBinding.isAssignableFrom(Long.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Integer.class, 38);
        } else if (fieldBinding.isAssignableFrom(Float.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Double.class, 38);
        } else if (fieldBinding.isAssignableFrom(Double.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Double.class, 38);
        } else if (fieldBinding.isAssignableFrom(Number.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Double.class, 38);
        } else if (fieldBinding.isAssignableFrom(Boolean.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Boolean.class, 4);
        } else if (fieldBinding.isAssignableFrom(Date.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Date.class, 8);
        } else if (fieldBinding.isAssignableFrom(Timestamp.class)) {
            schema = FeatureTypes.add(schema, this.fieldName, Timestamp.class, 8);
        } else if (Geometry.class.isAssignableFrom(fieldBinding)) {
            isGeometry = true;
            if (fieldBinding.isAssignableFrom(LinearRing.class)) {
                fieldBinding = LineString.class;
            }

            String typeName = delegate.getSchema().getTypeName();
            CoordinateReferenceSystem crs = delegate.getSchema().getCoordinateReferenceSystem();
            schema = FeatureTypes.getDefaultType(typeName, fieldBinding, crs);
            for (AttributeDescriptor dsc : delegate.getSchema().getAttributeDescriptors()) {
                if (dsc instanceof GeometryDescriptor) {
                    continue;
                }
                schema = FeatureTypes.add(schema, dsc);
            }
        } else {
            schema = FeatureTypes.add(schema, this.fieldName, String.class, 150);
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new FieldCalculationFeatureIterator(delegate.features(), getSchema(), expression,
                fieldName, isGeometry);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    static class FieldCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String fieldName;

        private SimpleFeatureBuilder builder;

        private Expression expression;

        private boolean isGeometry = false;

        public FieldCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression expression, String fieldName,
                boolean isGeometry) {
            this.delegate = delegate;

            this.expression = expression;
            this.fieldName = fieldName;
            this.isGeometry = isGeometry;
            this.builder = new SimpleFeatureBuilder(schema);
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
            SimpleFeature sourceFeature = delegate.next();
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());

            // transfer attributes
            transferAttribute(sourceFeature, nextFeature);

            Object value = expression.evaluate(sourceFeature);
            if (isGeometry) {
                nextFeature.setDefaultGeometry(value);
            } else {
                nextFeature.setAttribute(fieldName, value);
            }

            return nextFeature;
        }
    }
}
