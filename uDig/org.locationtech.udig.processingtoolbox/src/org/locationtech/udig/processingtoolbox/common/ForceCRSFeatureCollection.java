/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.common;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Force(Define) CRS(Projection) SimpleFeatureCollection Implementation
 * 
 * @author MapPlus, onspatial.com
 * @since 2012
 * @version $Id: ForceCRSFeatureCollection.java 1 2012-11-26 11:22:29Z minpa.lee $
 */
public class ForceCRSFeatureCollection extends DecoratingSimpleFeatureCollection {
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
                    if (geometry.getUserData() == null
                            || geometry.getUserData() instanceof CoordinateReferenceSystem) {
                        geometry.setUserData(forcedCRS);
                    }
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }
    }
}
