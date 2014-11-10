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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * General Feature Inserter
 * 
 * @author MapPlus
 * 
 */
public class FeatureInserter implements IFeatureInserter {
    protected static final Logger LOGGER = Logging.getLogger(FeatureInserter.class);

    static int FLUSH_INTERVAL = 2500;
    
    int featureCount = 0;

    Boolean isMemoryDataStore = Boolean.FALSE;

    String typeName;

    SimpleFeatureBuilder builder;

    ListFeatureCollection featureBuffer;

    int flushInterval = FLUSH_INTERVAL;

    SimpleFeatureStore sfStore = null;

    // ==== FeatureWriter mode ======
    Boolean isWriteMode = Boolean.FALSE;

    FeatureWriter<SimpleFeatureType, SimpleFeature> writer;

    DataStore dataStore;

    Transaction transaction;

    // ===============================

    List<FieldMap> fieldMaps = new ArrayList<FieldMap>();

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#getFeatureWriter()
     */
    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter() {
        return writer;
    }

    public FeatureInserter(DataStore dataStore, SimpleFeatureType featureType) {
        try {
            this.fieldMaps.clear();
            this.dataStore = dataStore;
            this.dataStore.createSchema(featureType);
            this.typeName = featureType.getTypeName();

            // call once
            dataStore.getFeatureSource(typeName);
            this.transaction = new DefaultTransaction(typeName); // Transaction.AUTO_COMMIT
            this.writer = dataStore.getFeatureWriterAppend(typeName, transaction);
            this.isWriteMode = Boolean.TRUE;
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    public FeatureInserter(SimpleFeatureStore featureStore) {
        this.fieldMaps.clear();
        this.sfStore = featureStore;
        this.transaction = featureStore.getTransaction();
        this.builder = new SimpleFeatureBuilder(featureStore.getSchema());
        this.featureBuffer = new ListFeatureCollection(featureStore.getSchema());
        this.isWriteMode = Boolean.FALSE;

        if (featureStore.getDataStore() instanceof MemoryDataStore) {
            isMemoryDataStore = Boolean.TRUE;
        }

        this.typeName = featureStore.getSchema().getTypeName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#getFeatureSource()
     */
    @Override
    public SimpleFeatureSource getFeatureSource() {
        if (isWriteMode) {
            try {
                return dataStore.getFeatureSource(writer.getFeatureType().getTypeName());
            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        } else {
            return sfStore;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#getFlushInterval()
     */
    @Override
    public int getFlushInterval() {
        return flushInterval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#setFlushInterval(int)
     */
    @Override
    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#getFeatureCount()
     */
    @Override
    public int getFeatureCount() {
        return featureCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#buildFeature(java.lang.String)
     */
    @Override
    public SimpleFeature buildFeature(String id) throws IOException {
        SimpleFeature simpleFeature = null;
        if (isWriteMode) {
            simpleFeature = this.writer.next();
        } else {
            simpleFeature = this.builder.buildFeature(id);
        }

        return simpleFeature;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#buildFeature()
     */
    @Override
    public SimpleFeature buildFeature() throws IOException {
        return buildFeature(null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.onspatial.gxt.common.IFeatureInserter#write(org.geotools.data.simple.SimpleFeatureCollection
     * )
     */
    @Override
    public void write(SimpleFeatureCollection featureCollection) throws IOException {
        if (isWriteMode) {
            SimpleFeatureIterator iter = featureCollection.features();
            try {
                while (iter.hasNext()) {
                    final SimpleFeature sf = iter.next();
                    SimpleFeature newFeature = this.buildFeature();
                    this.copyAttributes(sf, newFeature, true);
                    this.writer.write();
                }
            } finally {
                iter.close();
            }
        } else {
            featureCount += featureCollection.size();
            sfStore.addFeatures(featureCollection);

            if (!isMemoryDataStore) {
                transaction.commit();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.onspatial.gxt.common.IFeatureInserter#write(org.opengis.feature.simple.SimpleFeature)
     */
    @Override
    public void write(SimpleFeature newFeature) throws IOException {
        featureCount++;

        if (isWriteMode) {
            this.writer.write();
            if ((featureCount % flushInterval) == 0) {
                transaction.commit();
            }
        } else {
            featureBuffer.add(newFeature);
            if (flushInterval == featureBuffer.size()) {
                sfStore.addFeatures(featureBuffer);
                featureBuffer.clear();

                if (!isMemoryDataStore) {
                    transaction.commit();
                }
            }
        }
    }

    private void flush() throws IOException {
        if (isWriteMode) {
            transaction.commit();
        } else {
            if (featureBuffer.size() > 0) {
                sfStore.addFeatures(featureBuffer);
                featureBuffer.clear();
            }
            transaction.commit();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#rollback()
     */
    @Override
    public void rollback() throws IOException {
        if (isWriteMode) {
            // skip
        } else {
            transaction.rollback();
            featureBuffer.clear();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#rollback(java.lang.Exception)
     */
    @Override
    public void rollback(Exception e) throws IOException {
        rollback();
        LOGGER.log(Level.WARNING, e.getMessage(), e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#close()
     */
    @Override
    public void close() throws IOException {
        if (isWriteMode) {
            transaction.commit();
            writer.close();
            transaction.close();
        } else {
            flush();
            transaction.close();
            sfStore.setTransaction(Transaction.AUTO_COMMIT);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.onspatial.gxt.common.IFeatureInserter#close(org.geotools.data.simple.SimpleFeatureIterator
     * )
     */
    @Override
    public void close(SimpleFeatureIterator srcIter) throws IOException {
        close();

        if (srcIter != null) {
            srcIter.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.onspatial.gxt.common.IFeatureInserter#copyAttributes(org.opengis.feature.simple.SimpleFeature
     * , org.opengis.feature.simple.SimpleFeature, boolean)
     */
    @Override
    public SimpleFeature copyAttributes(SimpleFeature source, SimpleFeature target,
            boolean copyGeometry) {
        if (this.fieldMaps.size() == 0) {
            fieldMaps = FieldMap.buildMap(source.getFeatureType(), target.getFeatureType());
        }

        for (FieldMap fieldMap : this.fieldMaps) {
            if (fieldMap.isGeometry) {
                if (copyGeometry) {
                    target.setDefaultGeometry(source.getDefaultGeometry());
                }
            } else {
                target.setAttribute(fieldMap.destID, source.getAttribute(fieldMap.soruceID));
            }
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.onspatial.gxt.common.IFeatureInserter#clearFieldMaps()
     */
    @Override
    public void clearFieldMaps() {
        this.fieldMaps.clear();
    }
}
