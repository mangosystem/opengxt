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
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * General Feature Inserter
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MemoryFeatureInserter implements IFeatureInserter {
    protected static final Logger LOGGER = Logging.getLogger(MemoryFeatureInserter.class);

    int flushInterval = 0;

    int featureCount = 0;

    String typeName;

    SimpleFeatureBuilder builder;

    ListFeatureCollection features;

    List<FieldMap> fieldMaps = new ArrayList<FieldMap>();

    public MemoryFeatureInserter(SimpleFeatureType schema) {
        this.fieldMaps.clear();
        this.builder = new SimpleFeatureBuilder(schema);
        this.features = new ListFeatureCollection(schema);
        this.typeName = schema.getTypeName();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter() {
        return null;
    }

    @Override
    public SimpleFeatureSource getFeatureSource() {
        return DataUtilities.source(features);
    }

    @Override
    public SimpleFeatureCollection getFeatureCollection() throws IOException {
        return features;
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
        StringBuilder sb = new StringBuilder().append(typeName).append(".");
        String id = sb.append(featureCount + 1).toString();
        return builder.buildFeature(id);
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
        features.add(newFeature);
    }

    @Override
    public void rollback() throws IOException {
        // nothing to do
    }

    @Override
    public void rollback(Exception e) throws IOException {
        // nothing to do
    }

    @Override
    public void close() throws IOException {
        // nothing to do
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
