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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * AreaCalculation SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AreaCalculationFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(AreaCalculationFeatureCollection.class);

    private String areaField;

    private String perimeterField;

    private SimpleFeatureType schema;

    public AreaCalculationFeatureCollection(SimpleFeatureCollection delegate, String areaField) {
        this(delegate, areaField, null);
    }

    public AreaCalculationFeatureCollection(SimpleFeatureCollection delegate, String areaField,
            String perimeterField) {
        super(delegate);

        if (areaField == null || areaField.isEmpty()) {
            throw new NullPointerException("Area field is null");
        }

        this.areaField = areaField;
        this.perimeterField = perimeterField;

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.schema = FeatureTypes.add(schema, areaField, Double.class, 38);
        if (perimeterField != null && !perimeterField.isEmpty()) {
            schema = FeatureTypes.add(schema, perimeterField, Double.class, 38);
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new AreaCalculationFeatureIterator(delegate.features(), getSchema(), areaField,
                perimeterField);
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

    static class AreaCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String areaField;

        private String perimeterField;

        private boolean hasLengthField = false;

        private SimpleFeatureBuilder builder;

        public AreaCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, String areaField, String perimeterField) {
            this.delegate = delegate;

            this.areaField = areaField;
            this.perimeterField = perimeterField;
            this.hasLengthField = perimeterField != null && !perimeterField.isEmpty();
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
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());

            // transfer attributes
            transferAttribute(sourceFeature, nextFeature);

            // calculate area & perimeter
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
            nextFeature.setAttribute(areaField, geometry.getArea());
            if (hasLengthField) {
                nextFeature.setAttribute(perimeterField, geometry.getLength());
            }
            return nextFeature;
        }
    }
}
