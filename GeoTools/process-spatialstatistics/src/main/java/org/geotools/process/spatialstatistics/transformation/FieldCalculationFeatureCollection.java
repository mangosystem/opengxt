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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.expression.Expression;

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

    private Class<?> fieldBinding = Double.class; // default

    private SimpleFeatureType schema;

    public FieldCalculationFeatureCollection(SimpleFeatureCollection delegate, String fieldName,
            Expression expression) {
        super(delegate);

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.fieldName = FeatureTypes.validateProperty(schema, fieldName);
        this.expression = expression;

        if (FeatureTypes.existProeprty(schema, this.fieldName)) {
            AttributeDescriptor attributeType = schema.getDescriptor(this.fieldName);
            fieldBinding = attributeType.getType().getBinding();
        } else {
            // test value type
            SimpleFeatureIterator featureIter = null;
            fieldBinding = null;
            try {
                featureIter = delegate.features();
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
                schema = FeatureTypes.add(schema, fieldName, String.class, 150);
            } else if (fieldBinding.isAssignableFrom(Integer.class)) {
                schema = FeatureTypes.add(schema, fieldName, Integer.class, 38);
            } else if (fieldBinding.isAssignableFrom(Long.class)) {
                schema = FeatureTypes.add(schema, fieldName, Integer.class, 38);
            } else if (fieldBinding.isAssignableFrom(Float.class)) {
                schema = FeatureTypes.add(schema, fieldName, Double.class, 38);
            } else if (fieldBinding.isAssignableFrom(Double.class)) {
                schema = FeatureTypes.add(schema, fieldName, Double.class, 38);
            } else if (fieldBinding.isAssignableFrom(Number.class)) {
                schema = FeatureTypes.add(schema, fieldName, Double.class, 38);
            } else {
                schema = FeatureTypes.add(schema, fieldName, String.class, 150);
            }
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new FieldCalculationFeatureIterator(delegate.features(), getSchema(), fieldName,
                expression);
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

        public FieldCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, String fieldName, Expression expression) {
            this.delegate = delegate;

            this.fieldName = fieldName;
            this.expression = expression;
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
            nextFeature.setAttribute(fieldName, value);

            return nextFeature;
        }
    }
}
