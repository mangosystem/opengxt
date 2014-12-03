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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Force(Define) CRS(Projection) SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class ForceCRSFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ForceCRSFeatureCollection.class);

    private CoordinateReferenceSystem forcedCRS;

    private SimpleFeatureType schema;

    public ForceCRSFeatureCollection(SimpleFeatureCollection delegate,
            CoordinateReferenceSystem forcedCRS) {
        super(delegate);

        SimpleFeatureType schema = delegate.getSchema();
        this.schema = FeatureTypes.build(schema, schema.getTypeName(), forcedCRS);
        this.forcedCRS = forcedCRS;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ForceCRSFeatureIterator(delegate.features(), getSchema(), forcedCRS);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    public ReferencedEnvelope getBounds() {
        return new ReferencedEnvelope(delegate.getBounds(), schema.getCoordinateReferenceSystem());
    }

    static class ForceCRSFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private CoordinateReferenceSystem forcedCRS;

        private SimpleFeatureBuilder builder;

        public ForceCRSFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                CoordinateReferenceSystem forcedCRS) {
            this.delegate = delegate;
            this.forcedCRS = forcedCRS;
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
                    geometry.setUserData(forcedCRS);
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }
    }
}
