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
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Flip Line SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FlipLineFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(FlipLineFeatureCollection.class);

    public FlipLineFeatureCollection(SimpleFeatureCollection delegate) {
        super(delegate);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new FlipLineFeatureIterator(delegate.features(), getSchema());
    }

    static class FlipLineFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private SimpleFeatureBuilder builder;

        public FlipLineFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(schema);
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
                    attribute = geometry.reverse();
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }
    }
}
