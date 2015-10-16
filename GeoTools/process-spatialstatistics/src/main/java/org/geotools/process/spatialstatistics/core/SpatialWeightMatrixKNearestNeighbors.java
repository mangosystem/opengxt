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
 * K Nearest Neighbors (KNN) is a distance-based definition of neighbors where "k" refers to the
 * number of neighbors of a location. It is computed as the distance between a point and the number
 * (k) of nearest neighbor points (i.e. the distance between the central points of polygons).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightMatrixKNearestNeighbors extends AbstractSpatialWeightMatrix {
    protected static final Logger LOGGER = Logging
            .getLogger(SpatialWeightMatrixKNearestNeighbors.class);

    private int numberOfNeighbors = 4; // default value

    public int getNumberOfNeighbors() {
        return numberOfNeighbors;
    }

    public void setNumberOfNeighbors(int numberOfNeighbors) {
        this.numberOfNeighbors = numberOfNeighbors;
    }

    public SpatialWeightMatrixKNearestNeighbors() {

    }

    @Override
    public SpatialWeightMatrixResult execute(SimpleFeatureCollection features, String uniqueField) {
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Distance);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);

        int featureCount = 0;
        Envelope env = new Envelope();

        // 1. extract centroid and build spatial index
        KdTree spatialIndex = new KdTree(0.0d);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();
                Object primaryID = feature.getAttribute(uniqueField);

                spatialIndex.insert(coordinate, primaryID);
                featureCount++;
                env.expandToInclude(coordinate);
            }
        } finally {
            featureIter.close();
        }

        if (numberOfNeighbors >= featureCount) {
            featureIter = features.features();
            try {
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Object primaryID = feature.getAttribute(uniqueField);
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = geometry.getCentroid().getCoordinate();

                    SimpleFeatureIterator subIter = features.features();
                    try {
                        while (subIter.hasNext()) {
                            SimpleFeature secondaryFeature = subIter.next();
                            Object secondaryID = secondaryFeature.getAttribute(uniqueField);
                            if (primaryID.equals(secondaryID)) {
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
            double maxWidth = env.getWidth() > env.getHeight() ? env.getWidth() : env.getHeight();
            final double initDistance = maxWidth / Math.sqrt(featureCount);

            // 3.create spatial weight matrix
            List<KdNode> result = new ArrayList<KdNode>();
            TreeMap<Double, Object> sortedMap = new TreeMap<Double, Object>();
            int count = 0;

            featureIter = features.features();
            try {
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = geometry.getCentroid().getCoordinate();
                    Object primaryID = feature.getAttribute(uniqueField);

                    // init
                    env.init(coordinate);
                    sortedMap.clear();

                    // query
                    count = 0;
                    while (numberOfNeighbors > sortedMap.size()) {
                        double expandDistance = initDistance * ++count;
                        env.expandBy(expandDistance);

                        result.clear();
                        spatialIndex.query(env, result);
                        for (KdNode node : result) {
                            double distance = coordinate.distance(node.getCoordinate());
                            if (distance > expandDistance) {
                                continue;
                            }

                            Object secondaryID = node.getData();
                            if (primaryID.equals(secondaryID)
                                    || sortedMap.containsValue(secondaryID)) {
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
}
