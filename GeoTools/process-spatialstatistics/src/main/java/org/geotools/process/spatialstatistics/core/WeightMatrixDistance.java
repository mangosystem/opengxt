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

import java.util.Iterator;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

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

    @SuppressWarnings("unused")
    private double exponent = 1.0; // 1 or 2

    private STRtree spatialIndex;

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

                for (@SuppressWarnings("unchecked")
                Iterator<SpatialEvent> iter = (Iterator<SpatialEvent>) spatialIndex.query(queryEnv)
                        .iterator(); iter.hasNext();) {
                    SpatialEvent sample = iter.next();

                    double distance = coordinate.distance(sample.coordinate);
                    if (!this.isSelfNeighbors()
                            && (primaryID.equals(sample.id) || distance > thresholdDistance)) {
                        continue;
                    }

                    matrix.visit(primaryID, sample.id, distance);
                }
            }
        } finally {
            featureIter.close();
        }

        return matrix;
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
            }
        } finally {
            featureIter.close();
        }
    }
}
