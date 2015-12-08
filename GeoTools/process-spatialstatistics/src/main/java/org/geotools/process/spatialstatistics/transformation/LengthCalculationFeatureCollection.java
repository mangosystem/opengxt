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

import com.vividsolutions.jts.geom.Geometry;

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

    private SimpleFeatureType schema;

    public LengthCalculationFeatureCollection(SimpleFeatureCollection delegate, String lengthField) {
        super(delegate);

        if (lengthField == null || lengthField.isEmpty()) {
            throw new NullPointerException("Length field is null");
        }

        this.lengthField = lengthField;
        
        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.schema = FeatureTypes.add(schema, lengthField, Double.class, 38);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new LengthCalculationFeatureIterator(delegate.features(), getSchema(), lengthField);
    }

    static class LengthCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String lengthField;

        private SimpleFeatureBuilder builder;

        public LengthCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, String lengthField) {
            this.delegate = delegate;

            this.lengthField = lengthField;
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

            // calculate length
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
            nextFeature.setAttribute(lengthField, geometry.getLength());
            return nextFeature;
        }
    }
}
