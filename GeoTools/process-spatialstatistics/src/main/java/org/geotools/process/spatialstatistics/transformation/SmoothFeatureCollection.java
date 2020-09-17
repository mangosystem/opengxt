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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Smooths the geometries in a line or polygon layer.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SmoothFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(SmoothFeatureCollection.class);

    private double fit = 0.0;

    public SmoothFeatureCollection(SimpleFeatureCollection lineFeatures, double fit) {
        super(lineFeatures);

        // The value between 0 and 1 (inclusive) specifying the tightness of fit of the smoothed boundary (0 is loose)
        this.fit = Math.max(0.0, Math.min(1.0, fit));
    }

    @Override
    public SimpleFeatureIterator features() {
        return new smoothFeatureIterator(delegate.features(), this.getSchema(), fit);
    }

    static class smoothFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private double fit = 1;

        private int count = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private String typeName;

        public smoothFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                double fit) {
            this.delegate = delegate;

            this.fit = fit;

            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature source = delegate.next();

                next = builder.buildFeature(buildID(typeName, ++count));

                // transfer attributes
                transferAttribute(source, next);

                Geometry geometry = (Geometry) source.getDefaultGeometry();

                // smooth
                Geometry smoothed = JTS.smooth(geometry, fit, geometry.getFactory());
                smoothed.setUserData(geometry.getUserData());

                next.setDefaultGeometry(smoothed);

                builder.reset();
            }
            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = next;
            next = null;
            return result;
        }
    }
}