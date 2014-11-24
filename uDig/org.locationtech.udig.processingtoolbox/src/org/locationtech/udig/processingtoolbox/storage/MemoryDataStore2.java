/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.storage;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * MemoryDataStore2
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public final class MemoryDataStore2 extends MemoryDataStore {

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(
            final String typeName, final Transaction transaction) throws IOException {
        return new FeatureWriter<SimpleFeatureType, SimpleFeature>() {
            SimpleFeatureType featureType = getSchema(typeName);

            Map<String, SimpleFeature> contents = features(typeName);

            Iterator<SimpleFeature> iterator = contents.values().iterator();

            SimpleFeature live = null;

            int internalUID = 0;

            SimpleFeature current = null; // current Feature returned to user

            public SimpleFeatureType getFeatureType() {
                return featureType;
            }

            public SimpleFeature next() throws IOException, NoSuchElementException {
                if (hasNext()) {
                    // existing content
                    live = iterator.next();

                    try {
                        current = SimpleFeatureBuilder.copy(live);
                    } catch (IllegalAttributeException e) {
                        throw new DataSourceException("Unable to edit " + live.getID() + " of "
                                + typeName);
                    }
                } else {
                    // new content
                    live = null;

                    try {
                        final String fid = typeName + "." + ++internalUID;
                        current = SimpleFeatureBuilder.template(featureType, fid);
                    } catch (IllegalAttributeException e) {
                        throw new DataSourceException("Unable to add additional Features of "
                                + typeName);
                    }
                }

                return current;
            }

            public void remove() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                if (current == null) {
                    throw new IOException("No feature available to remove");
                }

                if (live != null) {
                    // remove existing content
                    iterator.remove();
                    listenerManager.fireFeaturesRemoved(typeName, transaction,
                            new ReferencedEnvelope(live.getBounds()), true);
                    live = null;
                    current = null;
                } else {
                    // cancel add new content
                    current = null;
                }
            }

            public void write() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                if (current == null) {
                    throw new IOException("No feature available to write");
                }

                if (live != null) {
                    if (live.equals(current)) {
                        // no modifications made to current
                        live = null;
                        current = null;
                    } else {
                        // accept modifications
                        try {
                            live.setAttributes(current.getAttributes());
                        } catch (Exception e) {
                            throw new DataSourceException("Unable to accept modifications to "
                                    + live.getID() + " on " + typeName);
                        }

                        ReferencedEnvelope bounds = new ReferencedEnvelope();
                        bounds.expandToInclude(new ReferencedEnvelope(live.getBounds()));
                        bounds.expandToInclude(new ReferencedEnvelope(current.getBounds()));
                        listenerManager.fireFeaturesChanged(typeName, transaction, bounds, true);
                        live = null;
                        current = null;
                    }
                } else {
                    // add new content
                    contents.put(current.getID(), current);
                    listenerManager.fireFeaturesAdded(typeName, transaction,
                            new ReferencedEnvelope(current.getBounds()), true);
                    current = null;
                }
            }

            public boolean hasNext() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                return (iterator != null) && iterator.hasNext();
            }

            public void close() {
                if (iterator != null) {
                    iterator = null;
                }

                if (featureType != null) {
                    featureType = null;
                }

                contents = null;
                current = null;
                live = null;
            }
        };
    }

}
