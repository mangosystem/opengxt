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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.UnitCalculator;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * LengthCalculation SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LengthCalculationFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(LengthCalculationFeatureCollection.class);

    private String lengthField;

    private DistanceUnit lengthUnit = DistanceUnit.Default;

    private SimpleFeatureType schema;

    public LengthCalculationFeatureCollection(SimpleFeatureCollection delegate, String lengthField) {
        this(delegate, lengthField, DistanceUnit.Default);
    }

    public LengthCalculationFeatureCollection(SimpleFeatureCollection delegate, String lengthField,
            DistanceUnit lengthUnit) {
        super(delegate);

        if (lengthField == null || lengthField.isEmpty()) {
            throw new NullPointerException("Length field is null");
        }

        this.lengthField = lengthField;
        this.lengthUnit = lengthUnit;

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.schema = FeatureTypes.add(schema, lengthField, Double.class, 38);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new LengthCalculationFeatureIterator(delegate, getSchema(), lengthField, lengthUnit);
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

    static class LengthCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String lengthField;

        private DistanceUnit lengthUnit = DistanceUnit.Default;

        private SimpleFeatureBuilder builder;

        private UnitCalculator calculator;

        public LengthCalculationFeatureIterator(SimpleFeatureCollection delegate,
                SimpleFeatureType schema, String lengthField, DistanceUnit lengthUnit) {
            this.delegate = delegate.features();

            this.lengthField = lengthField;
            this.lengthUnit = lengthUnit;
            this.builder = new SimpleFeatureBuilder(schema);

            calculator = new UnitCalculator(schema.getCoordinateReferenceSystem());
            if (calculator.isGeographic()) {
                calculator.setupTransformation(delegate.getBounds());
            }
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

            // calculate length
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
            double length = calculator.getLength(geometry, lengthUnit);
            nextFeature.setAttribute(lengthField, length);
            return nextFeature;
        }
    }
}
