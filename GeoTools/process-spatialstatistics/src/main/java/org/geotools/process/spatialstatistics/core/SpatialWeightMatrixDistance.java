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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * SpatialWeightMatrix - Distance based weights
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightMatrixDistance extends AbstractSpatialWeightMatrix {
    protected static final Logger LOGGER = Logging
            .getLogger(SpatialWeightMatrixKNearestNeighbors.class);

    private double thresholdDistance = 0.0d;

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    private boolean rowStandardization = true;

    private boolean useExponent = false;

    private int exponent = 1;

    public double getThresholdDistance() {
        return thresholdDistance;
    }

    public void setThresholdDistance(double thresholdDistance) {
        this.thresholdDistance = thresholdDistance;
    }

    public DistanceMethod getDistanceMethod() {
        return distanceMethod;
    }

    public void setDistanceMethod(DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;
    }

    public boolean isRowStandardization() {
        return rowStandardization;
    }

    public void setRowStandardization(boolean rowStandardization) {
        this.rowStandardization = rowStandardization;
    }

    public boolean isUseExponent() {
        return useExponent;
    }

    public void setUseExponent(boolean useExponent) {
        this.useExponent = useExponent;
    }

    public int getExponent() {
        return exponent;
    }

    public void setExponent(int exponent) {
        this.exponent = exponent;
    }

    public SpatialWeightMatrixDistance() {

    }

    @Override
    public SpatialWeightMatrixResult execute(SimpleFeatureCollection features, String uniqueField) {
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Distance);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);

        if (thresholdDistance == 0) {
            DistanceFactory factory = DistanceFactory.newInstance();
            factory.DistanceType = distanceMethod;
            thresholdDistance = factory.getThresholDistance(features);
            LOGGER.log(Level.WARNING, "The default neighborhood search threshold was "
                    + thresholdDistance);
        }

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
            }
        } finally {
            featureIter.close();
        }

        List<KdNode> result = new ArrayList<KdNode>();
        featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Point centroid = geometry.getCentroid();
                Coordinate coordinate = centroid.getCoordinate();
                Object primaryID = feature.getAttribute(uniqueField);

                Envelope env = centroid.buffer(thresholdDistance).getEnvelopeInternal();
                result.clear();
                spatialIndex.query(env, result);
                for (KdNode node : result) {
                    Object secondaryID = node.getData();
                    double distance = coordinate.distance(node.getCoordinate());
                    if (!this.isSelfContains()
                            && (primaryID.equals(secondaryID) || distance > thresholdDistance)) {
                        continue;
                    }

                    swm.visit(primaryID, secondaryID, distance);
                }
            }
        } finally {
            featureIter.close();
        }

        return swm;
    }

}
