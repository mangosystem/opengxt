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
import java.util.List;
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

        List<NearFeature> hubs = loadNearFeatures(nearFeatures, nearIdField);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // find nearest hub
                NearFeature nearFeature = null;
                double minumumDistance = Double.MAX_VALUE;
                for (NearFeature currentHub : hubs) {
                    double currentDist = geometry.distance(currentHub.location);
                    if (minumumDistance > currentDist) {
                        minumumDistance = currentDist;
                        nearFeature = currentHub;
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);

                // create line: direction = spoke --> hub
                if (maximumDistance < minumumDistance) {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, null);
                    }
                    newFeature.setAttribute(DIST_FIELD, null);
                } else {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, nearFeature.id);
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

    private List<NearFeature> loadNearFeatures(SimpleFeatureCollection features, String idField) {
        List<NearFeature> nears = new ArrayList<NearFeature>();

        boolean hasID = idField != null && features.getSchema().indexOf(idField) != -1;
        int serialID = 0;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (hasID) {
                    nears.add(new NearFeature(geometry, feature.getAttribute(idField)));
                } else {
                    nears.add(new NearFeature(geometry, Integer.valueOf(++serialID)));
                }
            }
        } finally {
            featureIter.close();
        }
        return nears;
    }

    static final class NearFeature {

        public Geometry location;

        public Object id;

        public NearFeature() {

        }

        public NearFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }

}