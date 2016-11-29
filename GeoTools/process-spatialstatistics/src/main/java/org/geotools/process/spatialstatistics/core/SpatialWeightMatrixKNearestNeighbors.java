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
package org.geotools.process.spatialstatistics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * SpatialWeightMatrix - Distance based weights - k-Nearest Neighbors. <br>
 * K Nearest Neighbors (KNN) is a distance-based definition of neighbors where "k" refers to the number of neighbors of a location. It is computed as
 * the distance between a point and the number (k) of nearest neighbor points (i.e. the distance between the central points of polygons).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightMatrixKNearestNeighbors extends AbstractSpatialWeightMatrix {
    protected static final Logger LOGGER = Logging
            .getLogger(SpatialWeightMatrixKNearestNeighbors.class);

    private int numberOfNeighbors = 4; // default value

    private KdTree spatialIndex;

    private int featureCount = 0;

    private Envelope envelope = new Envelope();

    public SpatialWeightMatrixKNearestNeighbors() {

    }

    public int getNumberOfNeighbors() {
        return numberOfNeighbors;
    }

    public void setNumberOfNeighbors(int numberOfNeighbors) {
        this.numberOfNeighbors = numberOfNeighbors;
    }

    @Override
    public SpatialWeightMatrixResult execute(SimpleFeatureCollection features, String uniqueField) {
        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        this.uniqueFieldIsFID = uniqueField == null || uniqueField.isEmpty();
        
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Distance);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        // 1. extract centroid and build spatial index
        buildSpatialIndex(features, uniqueField);

        if (numberOfNeighbors >= featureCount) {
            SimpleFeatureIterator featureIter = features.features();
            try {
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Object primaryID = getFeatureID(feature, uniqueField);
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = geometry.getCentroid().getCoordinate();

                    SimpleFeatureIterator subIter = features.features();
                    try {
                        while (subIter.hasNext()) {
                            SimpleFeature secondaryFeature = subIter.next();
                            Object secondaryID = getFeatureID(secondaryFeature, uniqueField);
                            if (!this.isSelfContains() && primaryID.equals(secondaryID)) {
                                continue;
                            }

                            Geometry secGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                            Coordinate secCoord = secGeom.getCentroid().getCoordinate();
                            swm.visit(primaryID, secondaryID, coordinate.distance(secCoord));
                        }
                    } finally {
                        subIter.close();
                    }
                }
            } finally {
                featureIter.close();
            }
        } else {
            // 2. evaluate envelope
            double maxWidth = envelope.getWidth() > envelope.getHeight() ? envelope.getWidth()
                    : envelope.getHeight();
            final double initDistance = maxWidth / Math.sqrt(featureCount);

            // 3.create spatial weight matrix
            List<KdNode> result = new ArrayList<KdNode>();
            TreeMap<Double, Object> sortedMap = new TreeMap<Double, Object>();
            int count = 0;

            SimpleFeatureIterator featureIter = features.features();
            try {
                Envelope queryEnv = new Envelope();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = geometry.getCentroid().getCoordinate();
                    Object primaryID = getFeatureID(feature, uniqueField);

                    // init
                    queryEnv.init(coordinate);
                    sortedMap.clear();

                    // query
                    count = 0;
                    while (numberOfNeighbors > sortedMap.size()) {
                        double expandDistance = initDistance * ++count;
                        queryEnv.expandBy(expandDistance);

                        result.clear();
                        spatialIndex.query(queryEnv, result);
                        for (KdNode node : result) {
                            double distance = coordinate.distance(node.getCoordinate());
                            if (distance > expandDistance) {
                                continue;
                            }

                            Object secondaryID = node.getData();
                            if (!this.isSelfContains()
                                    && (primaryID.equals(secondaryID) || sortedMap
                                            .containsValue(secondaryID))) {
                                continue;
                            }

                            sortedMap.put(Double.valueOf(distance), secondaryID);
                        }
                    }

                    // build weight matrix
                    count = 0;
                    for (Entry<Double, Object> entry : sortedMap.entrySet()) {
                        swm.visit(primaryID, entry.getValue(), entry.getKey());
                        count++;
                        if (count == numberOfNeighbors) {
                            break;
                        }
                    }
                }
            } finally {
                featureIter.close();
            }
        }

        return swm;
    }

    private void buildSpatialIndex(SimpleFeatureCollection features, String uniqueField) {
        spatialIndex = new KdTree(0.0d);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();
                Object primaryID = getFeatureID(feature, uniqueField);

                spatialIndex.insert(coordinate, primaryID);
                featureCount++;
                envelope.expandToInclude(coordinate);
            }
        } finally {
            featureIter.close();
        }
    }
}
