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
import org.geotools.process.spatialstatistics.core.UnitCalculator;
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
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

    private AreaUnit areaUnit = AreaUnit.Default;

    private String perimeterField;

    private DistanceUnit perimeterUnit = DistanceUnit.Default;

    private SimpleFeatureType schema;

    public AreaCalculationFeatureCollection(SimpleFeatureCollection delegate, String areaField) {
        this(delegate, areaField, null);
    }

    public AreaCalculationFeatureCollection(SimpleFeatureCollection delegate, String areaField,
            String perimeterField) {
        this(delegate, areaField, AreaUnit.Default, perimeterField, DistanceUnit.Default);
    }

    public AreaCalculationFeatureCollection(SimpleFeatureCollection delegate, String areaField,
            AreaUnit areaUnit, String perimeterField, DistanceUnit perimeterUnit) {
        super(delegate);

        if (areaField == null || areaField.isEmpty()) {
            throw new NullPointerException("Area field is null");
        }

        this.areaField = areaField;
        this.areaUnit = areaUnit;
        this.perimeterField = perimeterField;
        this.perimeterUnit = perimeterUnit;

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.schema = FeatureTypes.add(schema, areaField, Double.class, 38);
        if (perimeterField != null && !perimeterField.isEmpty()) {
            schema = FeatureTypes.add(schema, perimeterField, Double.class, 38);
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new AreaCalculationFeatureIterator(delegate, getSchema(), areaField, areaUnit,
                perimeterField, perimeterUnit);
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

        private AreaUnit areaUnit = AreaUnit.Default;

        private String perimeterField;

        private DistanceUnit perimeterUnit = DistanceUnit.Default;

        private boolean hasLengthField = false;

        private SimpleFeatureBuilder builder;

        private UnitCalculator calculator;

        public AreaCalculationFeatureIterator(SimpleFeatureCollection delegate,
                SimpleFeatureType schema, String areaField, AreaUnit areaUnit,
                String perimeterField, DistanceUnit perimeterUnit) {
            this.delegate = delegate.features();

            this.areaField = areaField;
            this.areaUnit = areaUnit;
            this.perimeterField = perimeterField;
            this.perimeterUnit = perimeterUnit;
            this.hasLengthField = perimeterField != null && !perimeterField.isEmpty();
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

            // calculate area & perimeter
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();

            double area = calculator.getArea(geometry, areaUnit);
            nextFeature.setAttribute(areaField, area);
            if (hasLengthField) {
                double length = calculator.getLength(geometry, perimeterUnit);
                nextFeature.setAttribute(perimeterField, length);
            }
            return nextFeature;
        }
    }
}
