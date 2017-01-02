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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.spatialstatistics.enumeration.ContiguityType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

/**
 * SpatialWeightMatrix - Contiguity based weights
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WeightMatrixContiguity extends AbstractWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(WeightMatrixKNearestNeighbors.class);

    // Queen's default order, maximum = 12
    private int orderOfContiguity = 1;

    private ContiguityType contiguityType = ContiguityType.Queen;

    public WeightMatrixContiguity() {

    }

    public int getOrderOfContiguity() {
        return orderOfContiguity;
    }

    public void setOrderOfContiguity(int orderOfContiguity) {
        if (orderOfContiguity > 12) {
            orderOfContiguity = 12;
            LOGGER.log(Level.WARNING, "Maximum Order Of Contiguity is 12!");
        }
        this.orderOfContiguity = orderOfContiguity;
    }

    public ContiguityType getContiguityType() {
        return contiguityType;
    }

    public void setContiguityType(ContiguityType contiguityType) {
        this.contiguityType = contiguityType;
    }

    @Override
    public WeightMatrix execute(SimpleFeatureCollection features, String uniqueField) {
        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        this.uniqueFieldIsFID = uniqueField == null || uniqueField.isEmpty();

        // using SpatialIndexFeatureCollection
        SimpleFeatureCollection indexed = DataUtils.toSpatialIndexFeatureCollection(features);

        switch (contiguityType) {
        case Queen:
            return queen(indexed, uniqueField);
        case Rook:
            return rook(indexed, uniqueField);
        case Bishops:
            return bishops(indexed, uniqueField);
        default:
            return queen(indexed, uniqueField);
        }
    }

    // Polygon Contiguity (Edges and Corners)—A queen weights matrix defines a location's
    // neighbors as those with either a shared border or vertex
    private WeightMatrix queen(SimpleFeatureCollection features, String uniqueField) {
        WeightMatrix matrix = new WeightMatrix(SpatialWeightMatrixType.Contiguity);
        matrix.setupVariables(features.getSchema().getTypeName(), uniqueField);

        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = getFeatureID(primaryFeature, uniqueField);

                // spatial query
                // TODO orderOfContiguity
                Filter filter = getIntersectsFilter(the_geom, primaryGeometry);
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = getFeatureID(secondaryFeature, uniqueField);
                        if (!this.isSelfNeighbors() && primaryID.equals(secondaryID)) {
                            continue;
                        }

                        matrix.visit(primaryID, secondaryID);
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return matrix;
    }

    // Polygon Contiguity (Edges Only)—A rook weights matrix defines a location's neighbors as
    // those areas with shared borders
    private WeightMatrix rook(SimpleFeatureCollection features, String uniqueField) {
        WeightMatrix matrix = new WeightMatrix(SpatialWeightMatrixType.Contiguity);
        matrix.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = getFeatureID(primaryFeature, uniqueField);

                // spatial query
                Filter filter = getIntersectsFilter(the_geom, primaryGeometry);
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = getFeatureID(secondaryFeature, uniqueField);
                        if (!this.isSelfNeighbors() && primaryID.equals(secondaryID)) {
                            continue;
                        }

                        Geometry secondaryGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                        Geometry intersects = primaryGeometry.intersection(secondaryGeom);
                        if (intersects instanceof Point || intersects instanceof MultiPoint) {
                            continue;
                        }

                        matrix.visit(primaryID, secondaryID);
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return matrix;
    }

    // Polygon Contiguity (Corners Only)—A rook weights matrix defines a location's neighbors as
    // those areas with shared vertex
    private WeightMatrix bishops(SimpleFeatureCollection features, String uniqueField) {
        WeightMatrix matrix = new WeightMatrix(SpatialWeightMatrixType.Contiguity);
        matrix.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = getFeatureID(primaryFeature, uniqueField);

                // spatial query
                Filter filter = getIntersectsFilter(the_geom, primaryGeometry);
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = getFeatureID(secondaryFeature, uniqueField);
                        if (!this.isSelfNeighbors() && primaryID.equals(secondaryID)) {
                            continue;
                        }

                        Geometry secondaryGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                        Geometry intersects = primaryGeometry.intersection(secondaryGeom);
                        if (intersects instanceof Point || intersects instanceof MultiPoint) {
                            matrix.visit(primaryID, secondaryID);
                        }
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return matrix;
    }

    private Filter getIntersectsFilter(String geomField, Geometry searchGeometry) {
        return ff.and(ff.bbox(ff.property(geomField), JTS.toEnvelope(searchGeometry)),
                ff.intersects(ff.property(geomField), ff.literal(searchGeometry)));
    }
}
