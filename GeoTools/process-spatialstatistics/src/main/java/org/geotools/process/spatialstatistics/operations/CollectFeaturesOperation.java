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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Collect Features combines coincident point, line, polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class CollectFeaturesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(CollectFeaturesOperation.class);

    static final String COUNT_FIELD = "icount";

    private double tolerance = 0.1d;

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String countField)
            throws IOException {
        final SimpleShapeType geometryType = FeatureTypes.getSimpleShapeType(features);
        if (geometryType == SimpleShapeType.POINT) {
            CollectEventsOperation operation = new CollectEventsOperation();
            operation.setTolerance(getTolerance());
            return operation.execute(features, countField);
        }

        String typeName = features.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(features.getSchema(), typeName);
        if (countField == null || countField.isEmpty()) {
            countField = COUNT_FIELD;
        }
        schema = FeatureTypes.add(schema, countField, Integer.class);
        Class<?> outputBinding = schema.getDescriptor(countField).getType().getBinding();

        STRtree spatialIndex = buildIndex(features, geometryType);
        List<String> processedMap = new ArrayList<String>();

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                String featureID = feature.getID();
                if (processedMap.contains(featureID)) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Envelope searchEnv = geometry.getEnvelopeInternal();

                // count coincident events
                int featureCount = 1;
                for (@SuppressWarnings("unchecked")
                Iterator<Event> iter = (Iterator<Event>) spatialIndex.query(searchEnv)
                        .iterator(); iter.hasNext();) {
                    Event sample = iter.next();

                    if (processedMap.contains(sample.getFID()) || sample.equalsFID(featureID)) {
                        continue;
                    }

                    if (geometry.equalsExact(sample.geometry, tolerance)) {
                        featureCount++;
                        processedMap.add(sample.getFID());
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
                Object count = Converters.convert(featureCount, outputBinding);
                newFeature.setAttribute(countField, count);

                featureWriter.write(newFeature);
                processedMap.add(featureID);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree buildIndex(SimpleFeatureCollection features, SimpleShapeType geometryType) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Event event = new Event(feature.getID(), geometry);
                spatialIndex.insert(geometry.getEnvelopeInternal(), event);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    final class Event {

        private String fid;

        private Geometry geometry;

        public Event(String fid, Geometry geometry) {
            this.fid = fid;
            this.geometry = geometry;
        }

        public String getFID() {
            return this.fid;
        }

        public boolean equalsFID(String fid) {
            return this.fid.equals(fid);
        }

        public double distance(Geometry geometry) {
            return this.geometry.distance(geometry);
        }
    }

}
