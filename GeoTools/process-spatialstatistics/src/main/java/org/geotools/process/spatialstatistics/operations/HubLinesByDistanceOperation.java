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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

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

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures, boolean useCentroid, boolean preserveAttributes,
            double maximumDistance) throws IOException {

        this.setPreserveAttributes(preserveAttributes);
        this.setMaximumDistance(maximumDistance);
        this.setUseCentroid(useCentroid);

        return execute(hubFeatures, hubIdField, spokeFeatures);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures) throws IOException {
        SimpleFeatureType spokeSchema = spokeFeatures.getSchema();
        CoordinateReferenceSystem crs = spokeSchema.getCoordinateReferenceSystem();

        SimpleFeatureType featureType = null;
        if (preserveAttributes) {
            featureType = FeatureTypes.build(spokeSchema, TYPE_NAME, LineString.class);
        } else {
            String geomName = spokeSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName, LineString.class, crs);
        }

        boolean hasHubID = hubIdField != null && hubFeatures.getSchema().indexOf(hubIdField) != -1;
        if (hasHubID) {
            AttributeDescriptor hubIdDesc = hubFeatures.getSchema().getDescriptor(hubIdField);
            featureType = FeatureTypes.add(featureType, hubIdDesc);
        }
        featureType = FeatureTypes.add(featureType, HUB_DIST, Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        STRtree spatialIndex = loadHubs(hubFeatures, hubIdField);
        SimpleFeatureIterator spokeIter = spokeFeatures.features();
        try {
            while (spokeIter.hasNext()) {
                SimpleFeature feature = spokeIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object id = hasHubID ? feature.getAttribute(hubIdField) : feature.getID();
                if (useCentroid) {
                    geometry = geometry.getCentroid();
                }

                // find nearest hub
                Hub nearest = (Hub) spatialIndex.nearestNeighbour(geometry.getEnvelopeInternal(),
                        new Hub(geometry, id), new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                Hub s1 = (Hub) item1.getItem();
                                Hub s2 = (Hub) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                // create line: direction = spoke --> hub
                Geometry hubLine = getShortestLine(geometry, nearest.location, false);
                double distance = hubLine.getLength();
                if (distance == 0 || this.maximumDistance < distance) {
                    continue;
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                if (preserveAttributes) {
                    featureWriter.copyAttributes(feature, newFeature, false);
                }

                newFeature.setDefaultGeometry(hubLine);
                if (hasHubID) {
                    newFeature.setAttribute(hubIdField, nearest.id);
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

    private STRtree loadHubs(SimpleFeatureCollection features, String idField) {
        STRtree spatialIndex = new STRtree();
        boolean hasID = idField != null && features.getSchema().indexOf(idField) != -1;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (useCentroid) {
                    geometry = geometry.getCentroid();
                }

                Object id = hasID ? feature.getAttribute(idField) : feature.getID();
                Hub hub = new Hub(geometry, id);
                spatialIndex.insert(geometry.getEnvelopeInternal(), hub);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class Hub {

        public Geometry location;

        public Object id;

        public Hub(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}