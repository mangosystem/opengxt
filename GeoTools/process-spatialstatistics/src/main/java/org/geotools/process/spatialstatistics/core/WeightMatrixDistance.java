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
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * SpatialWeightMatrix - Distance based weights
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WeightMatrixDistance extends AbstractWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(WeightMatrixKNearestNeighbors.class);

    private double thresholdDistance = 0.0d;

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    private SpatialConcept spatialConcept = SpatialConcept.InverseDistance;

    private double exponent = 1.0; // 1 or 2

    private KdTree spatialIndex;

    public WeightMatrixDistance() {

    }

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

    public SpatialConcept getSpatialConcept() {
        return spatialConcept;
    }

    public void setSpatialConcept(SpatialConcept spatialConcept) {
        this.spatialConcept = spatialConcept;
        this.exponent = spatialConcept == SpatialConcept.InverseDistanceSquared ? 2.0 : 1.0;
    }

    @Override
    public WeightMatrix execute(SimpleFeatureCollection features, String uniqueField) {
        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        this.uniqueFieldIsFID = uniqueField == null || uniqueField.isEmpty();

        WeightMatrix matrix = new WeightMatrix(SpatialWeightMatrixType.Distance);
        matrix.setupVariables(features.getSchema().getTypeName(), uniqueField);
        
        // 1. extract centroid and build spatial index
        this.buildSpatialIndex(features, uniqueField);

        List<KdNode> result = new ArrayList<KdNode>();
        SimpleFeatureIterator featureIter = features.features();
        try {
            Envelope queryEnv = new Envelope();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();
                Object primaryID = getFeatureID(feature, uniqueField);

                queryEnv.init(coordinate);
                queryEnv.expandBy(thresholdDistance);

                result.clear();
                spatialIndex.query(queryEnv, result);
                for (KdNode node : result) {
                    Object secondaryID = node.getData();
                    double distance = coordinate.distance(node.getCoordinate());
                    if (!this.isSelfNeighbors()
                            && (primaryID.equals(secondaryID) || distance > thresholdDistance)) {
                        continue;
                    }

                    matrix.visit(primaryID, secondaryID, distance);
                }
            }
        } finally {
            featureIter.close();
        }

        return matrix;
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
            }
        } finally {
            featureIter.close();
        }
    }
}
