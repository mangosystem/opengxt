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
package org.geotools.process.spatialstatistics.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;

/**
 * General Feature Inserter
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureInserter implements IFeatureInserter {
    protected static final Logger LOGGER = Logging.getLogger(FeatureInserter.class);

    int featureCount = 0;

    Boolean isMemoryDataStore = Boolean.FALSE;

    String typeName;

    SimpleFeatureBuilder builder;

    ListFeatureCollection featureBuffer;

    int flushInterval = 2500;

    SimpleFeatureStore sfStore = null;

    Boolean writeMode = Boolean.FALSE;

    FeatureWriter<SimpleFeatureType, SimpleFeature> writer;

    DataStore dataStore;

    Transaction transaction;

    List<FieldMap> fieldMaps = new ArrayList<FieldMap>();

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
            this.writeMode = Boolean.TRUE;
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
        this.writeMode = Boolean.FALSE;

        if (featureStore.getDataStore() instanceof MemoryDataStore) {
            isMemoryDataStore = Boolean.TRUE;
        }

        this.typeName = featureStore.getSchema().getTypeName();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter() {
        return writer;
    }

    @Override
    public SimpleFeatureSource getFeatureSource() {
        if (writeMode) {
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

    @Override
    public SimpleFeatureCollection getFeatureCollection() throws IOException {
        SimpleFeatureSource featureSource = getFeatureSource();
        if (featureSource == null) {
            return null;
        } else {
            return featureSource.getFeatures();
        }
    }

    @Override
    public int getFlushInterval() {
        return flushInterval;
    }

    @Override
    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    @Override
    public int getFeatureCount() {
        return featureCount;
    }

    @Override
    public SimpleFeature buildFeature() throws IOException {
        SimpleFeature simpleFeature = null;
        if (writeMode) {
            simpleFeature = this.writer.next();
        } else {
            StringBuilder sb = new StringBuilder().append(typeName).append(".");
            String id = sb.append(featureCount + 1).toString();
            simpleFeature = this.builder.buildFeature(id);
        }

        return simpleFeature;
    }

    @Override
    public void write(SimpleFeatureCollection featureCollection) throws IOException {
        SimpleFeatureIterator iter = featureCollection.features();
        try {
            while (iter.hasNext()) {
                this.write(iter.next());
            }
        } finally {
            iter.close();
        }
    }

    @Override
    public void write(SimpleFeature newFeature) throws IOException {
        featureCount++;

        if (writeMode) {
            this.writer.write();
            if ((featureCount % flushInterval) == 0) {
                transaction.commit();
            }
        } else {
            featureBuffer.add(newFeature);
            if (flushInterval == featureBuffer.size()) {
                sfStore.addFeatures(featureBuffer);
                featureBuffer.clear();
                transaction.commit();
            }
        }
    }

    private void flush() throws IOException {
        if (!writeMode && featureBuffer.size() > 0) {
            sfStore.addFeatures(featureBuffer);
            featureBuffer.clear();
        }
        transaction.commit();
    }

    @Override
    public void rollback() throws IOException {
        transaction.rollback();
        if (!writeMode) {
            featureBuffer.clear();
        }
    }

    @Override
    public void rollback(Exception e) throws IOException {
        rollback();
        LOGGER.log(Level.WARNING, e.getMessage(), e);
    }

    @Override
    public void close() throws IOException {
        flush();
        transaction.close();

        if (writeMode) {
            writer.close();
        } else {
            sfStore.setTransaction(Transaction.AUTO_COMMIT);
        }
    }

    @Override
    public void close(SimpleFeatureIterator iterator) throws IOException {
        close();

        if (iterator != null) {
            iterator.close();
        }
    }

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

    @Override
    public void clearFieldMaps() {
        this.fieldMaps.clear();
    }
}
