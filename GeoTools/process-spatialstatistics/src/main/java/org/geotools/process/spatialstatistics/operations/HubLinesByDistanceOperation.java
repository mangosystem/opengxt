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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by nearest distance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class HubLinesByDistanceOperation extends AbstractHubLinesOperation {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByDistanceOperation.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection spokeFeatures,
            SimpleFeatureCollection hubFeatures, String hubIdField, boolean useCentroid,
            boolean preserveAttributes, double maximumDistance) throws IOException {

        this.setPreserveAttributes(preserveAttributes);
        this.setMaximumDistance(maximumDistance);
        this.setUseCentroid(useCentroid);

        return execute(spokeFeatures, hubFeatures, hubIdField);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection spokeFeatures,
            SimpleFeatureCollection hubFeatures, String hubIdField) throws IOException {
        SimpleFeatureType spokeSchema = spokeFeatures.getSchema();
        CoordinateReferenceSystem crs = spokeSchema.getCoordinateReferenceSystem();

        SimpleFeatureType featureType = null;
        if (preserveAttributes) {
            featureType = FeatureTypes.build(spokeSchema, "HubLines", LineString.class);
        } else {
            String geomName = spokeSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType("HubLines", geomName, LineString.class, crs);
        }

        boolean hasHubID = hubIdField != null && hubFeatures.getSchema().indexOf(hubIdField) != -1;
        if (hasHubID) {
            AttributeDescriptor hubIdDesc = hubFeatures.getSchema().getDescriptor(hubIdField);
            featureType = FeatureTypes.add(featureType, hubIdDesc);
        }
        featureType = FeatureTypes.add(featureType, HUB_DIST, Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        List<Hub> hubs = loadHubs(hubFeatures, hubIdField);
        SimpleFeatureIterator spokeIter = spokeFeatures.features();
        try {
            while (spokeIter.hasNext()) {
                SimpleFeature spokeFeature = spokeIter.next();
                Geometry spokeGeom = (Geometry) spokeFeature.getDefaultGeometry();
                if (useCentroid) {
                    spokeGeom = spokeGeom.getCentroid();
                }

                // find nearest hub
                Hub nearestHub = null;
                double minDist = Double.MAX_VALUE;
                for (Hub currentHub : hubs) {
                    double currentDist = currentHub.location.distance(spokeGeom);
                    if (minDist > currentDist) {
                        minDist = currentDist;
                        nearestHub = currentHub;
                    }
                }

                // create line: direction = spoke --> hub
                Geometry hubLine = getShortestLine(spokeGeom, nearestHub.location, false);
                double distance = hubLine.getLength();
                if (distance == 0 || this.maximumDistance < distance) {
                    continue;
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                if (preserveAttributes) {
                    featureWriter.copyAttributes(spokeFeature, newFeature, false);
                }

                newFeature.setDefaultGeometry(hubLine);
                if (hasHubID) {
                    newFeature.setAttribute(hubIdField, nearestHub.id);
                }
                newFeature.setAttribute(HUB_DIST, distance);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(spokeIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private List<Hub> loadHubs(SimpleFeatureCollection hubFeatures, String hubIdField) {
        List<Hub> hubs = new ArrayList<Hub>();

        boolean hasHubID = hubIdField != null && hubFeatures.getSchema().indexOf(hubIdField) != -1;
        int serialID = 0;

        SimpleFeatureIterator spokeIter = hubFeatures.features();
        try {
            while (spokeIter.hasNext()) {
                SimpleFeature spokebFeature = spokeIter.next();
                Geometry spokeGeom = (Geometry) spokebFeature.getDefaultGeometry();
                if (useCentroid) {
                    spokeGeom = spokeGeom.getCentroid();
                }

                if (hasHubID) {
                    hubs.add(new Hub(spokeGeom, spokebFeature.getAttribute(hubIdField)));
                } else {
                    hubs.add(new Hub(spokeGeom, Integer.valueOf(++serialID)));
                }
            }
        } finally {
            spokeIter.close();
        }
        return hubs;
    }

    static final class Hub {

        public Geometry location;

        public Object id;

        public Hub() {

        }

        public Hub(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }

}