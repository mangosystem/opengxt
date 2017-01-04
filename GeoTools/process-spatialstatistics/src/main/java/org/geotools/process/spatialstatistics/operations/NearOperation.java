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
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Calculates distance and additional proximity information between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class NearOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(NearOperation.class);

    protected static final String DIST_FIELD = "dist";

    protected double maximumDistance = Double.MAX_VALUE;

    public void setMaximumDistance(double maximumDistance) {
        if (Double.isNaN(maximumDistance) || Double.isInfinite(maximumDistance)
                || maximumDistance == 0) {
            this.maximumDistance = Double.MAX_VALUE;
        } else {
            this.maximumDistance = maximumDistance;
        }
    }

    public double getMaximumDistance() {
        return maximumDistance;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField, double maximumDistance)
            throws IOException {
        this.setMaximumDistance(maximumDistance);
        return execute(inputFeatures, nearFeatures, nearIdField);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField) throws IOException {
        SimpleFeatureType inputSchema = inputFeatures.getSchema();

        SimpleFeatureType featureType = null;
        featureType = FeatureTypes.build(inputSchema, inputSchema.getTypeName());

        boolean hasID = nearIdField != null && nearFeatures.getSchema().indexOf(nearIdField) != -1;
        if (hasID) {
            AttributeDescriptor nearIdDesc = nearFeatures.getSchema().getDescriptor(nearIdField);
            featureType = FeatureTypes.add(featureType, nearIdDesc);
        }
        featureType = FeatureTypes.add(featureType, DIST_FIELD, Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        STRtree spatialIndex = loadNearFeatures(nearFeatures, nearIdField);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object id = hasID ? feature.getAttribute(nearIdField) : feature.getID();
                NearFeature source = new NearFeature(geometry, id);

                // find nearest hub
                NearFeature nearest = (NearFeature) spatialIndex.nearestNeighbour(
                        geometry.getEnvelopeInternal(), source, new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                NearFeature s1 = (NearFeature) item1.getItem();
                                NearFeature s2 = (NearFeature) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                double minumumDistance = source.location.distance(nearest.location);

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                if (maximumDistance < minumumDistance) {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, null);
                    }
                    newFeature.setAttribute(DIST_FIELD, null);
                } else {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, nearest.id);
                    }
                    newFeature.setAttribute(DIST_FIELD, minumumDistance);
                }

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree loadNearFeatures(SimpleFeatureCollection features, String idField) {
        STRtree spatialIndex = new STRtree();
        boolean hasID = idField != null && features.getSchema().indexOf(idField) != -1;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object id = hasID ? feature.getAttribute(idField) : feature.getID();
                NearFeature nearFeature = new NearFeature(geometry, id);
                spatialIndex.insert(geometry.getEnvelopeInternal(), nearFeature);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class NearFeature {

        public Geometry location;

        public Object id;

        public NearFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}