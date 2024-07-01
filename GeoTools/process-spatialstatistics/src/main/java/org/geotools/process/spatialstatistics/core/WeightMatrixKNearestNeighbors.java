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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * SpatialWeightMatrix - Distance based weights - k-Nearest Neighbors. <br>
 * K Nearest Neighbors (KNN) is a distance-based definition of neighbors where "k" refers to the number of neighbors of a location. It is computed as
 * the distance between a point and the number (k) of nearest neighbor points (i.e. the distance between the central points of polygons).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WeightMatrixKNearestNeighbors extends AbstractWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(WeightMatrixKNearestNeighbors.class);

    // default number of neighbors = 8, maximum = 24
    private int numberOfNeighbors = 8;

    private STRtree spatialIndex;

    private int featureCount = 0;

    public WeightMatrixKNearestNeighbors() {

    }

    public int getNumberOfNeighbors() {
        return numberOfNeighbors;
    }

    public void setNumberOfNeighbors(int numberOfNeighbors) {
        if (numberOfNeighbors > 24) {
            numberOfNeighbors = 24;
            LOGGER.log(Level.WARNING, "Maximum number of neighbors is 24!");
        }
        this.numberOfNeighbors = numberOfNeighbors;
    }

    @Override
    public WeightMatrix execute(SimpleFeatureCollection features, String uniqueField) {
        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        this.uniqueFieldIsFID = uniqueField == null || uniqueField.isEmpty();

        WeightMatrix matrix = new WeightMatrix(SpatialWeightMatrixType.Distance);
        matrix.setupVariables(features.getSchema().getTypeName(), uniqueField);

        // 1. extract centroid and build spatial index
        this.buildSpatialIndex(features, uniqueField);

        if (numberOfNeighbors >= featureCount) {
            insertAll(matrix, features, uniqueField);
        } else {
            SimpleFeatureIterator featureIter = features.features();
            try {
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = geometry.getCentroid().getCoordinate();
                    Object primaryID = getFeatureID(feature, uniqueField);

                    SpatialEvent soruce = new SpatialEvent(primaryID, coordinate);
                    Object[] knns = spatialIndex.nearestNeighbour(new Envelope(coordinate), soruce,
                            new ItemDistance() {
                                @Override
                                public double distance(ItemBoundable item1, ItemBoundable item2) {
                                    SpatialEvent s1 = (SpatialEvent) item1.getItem();
                                    SpatialEvent s2 = (SpatialEvent) item2.getItem();
                                    if (!isSelfNeighbors() && s1.id.equals(s2.id)) {
                                        return Double.MAX_VALUE;
                                    }
                                    return s1.distance(s2);
                                }
                            }, numberOfNeighbors);

                    // build weight matrix
                    for (Object object : knns) {
                        SpatialEvent current = (SpatialEvent) object;
                        matrix.visit(primaryID, current.id, soruce.distance(current));
                    }
                }
            } finally {
                featureIter.close();
            }
        }

        return matrix;
    }

    private void insertAll(WeightMatrix matrix, SimpleFeatureCollection features, String uniqueField) {
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
                        if (!this.isSelfNeighbors() && primaryID.equals(secondaryID)) {
                            continue;
                        }

                        Geometry secGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                        Coordinate secCoord = secGeom.getCentroid().getCoordinate();
                        matrix.visit(primaryID, secondaryID, coordinate.distance(secCoord));
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }

    }

    private void buildSpatialIndex(SimpleFeatureCollection features, String uniqueField) {
        spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate centroid = geometry.getCentroid().getCoordinate();
                Object primaryID = getFeatureID(feature, uniqueField);

                spatialIndex.insert(new Envelope(centroid), new SpatialEvent(primaryID, centroid));
                featureCount++;
            }
        } finally {
            featureIter.close();
        }
    }
}
