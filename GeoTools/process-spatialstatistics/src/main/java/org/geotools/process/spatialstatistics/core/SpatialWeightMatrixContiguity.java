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

import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
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
public class SpatialWeightMatrixContiguity extends AbstractSpatialWeightMatrix {
    protected static final Logger LOGGER = Logging
            .getLogger(SpatialWeightMatrixKNearestNeighbors.class);

    private int orderOfContiguity = 1; // Queen's default order

    private ContiguityType contiguityType = ContiguityType.Queen;

    public int getOrderOfContiguity() {
        return orderOfContiguity;
    }

    public void setOrderOfContiguity(int orderOfContiguity) {
        this.orderOfContiguity = orderOfContiguity;
    }

    public ContiguityType getContiguityType() {
        return contiguityType;
    }

    public void setContiguityType(ContiguityType contiguityType) {
        this.contiguityType = contiguityType;
    }

    public SpatialWeightMatrixContiguity() {

    }

    @Override
    public SpatialWeightMatrixResult execute(SimpleFeatureCollection features, String uniqueField) {
        switch (contiguityType) {
        case Queen:
            return queen(features, uniqueField);
        case Rook:
            return rook(features, uniqueField);
        case Bishops:
            return bishops(features, uniqueField);
        default:
            return queen(features, uniqueField);
        }
    }

    // Polygon Contiguity (Edges and Corners)—A queen weights matrix defines a location's
    // neighbors as those with either a shared border or vertex
    private SpatialWeightMatrixResult queen(SimpleFeatureCollection features, String uniqueField) {
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Contiguity);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = primaryFeature.getAttribute(uniqueField);

                // spatial query
                // TODO orderOfContiguity
                Filter filter = ff.intersects(ff.property(the_geom), ff.literal(primaryGeometry));
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = secondaryFeature.getAttribute(uniqueField);
                        if (primaryID.equals(secondaryID)) {
                            continue;
                        }

                        swm.visit(primaryID, secondaryID);
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return swm;
    }

    // Polygon Contiguity (Edges Only)—A rook weights matrix defines a location's neighbors as
    // those areas with shared borders
    private SpatialWeightMatrixResult rook(SimpleFeatureCollection features, String uniqueField) {
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Contiguity);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = primaryFeature.getAttribute(uniqueField);

                // spatial query
                Filter filter = ff.intersects(ff.property(the_geom), ff.literal(primaryGeometry));
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = secondaryFeature.getAttribute(uniqueField);
                        if (primaryID.equals(secondaryID)) {
                            continue;
                        }

                        Geometry secondaryGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                        Geometry intersects = primaryGeometry.intersection(secondaryGeom);
                        if (intersects instanceof Point || intersects instanceof MultiPoint) {
                            continue;
                        }

                        swm.visit(primaryID, secondaryID);
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return swm;
    }

    // Polygon Contiguity (Corners Only)—A rook weights matrix defines a location's neighbors as
    // those areas with shared vertex
    private SpatialWeightMatrixResult bishops(SimpleFeatureCollection features, String uniqueField) {
        SpatialWeightMatrixResult swm = new SpatialWeightMatrixResult(
                SpatialWeightMatrixType.Contiguity);
        swm.setupVariables(features.getSchema().getTypeName(), uniqueField);

        uniqueField = FeatureTypes.validateProperty(features.getSchema(), uniqueField);
        final String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature primaryFeature = featureIter.next();
                Geometry primaryGeometry = (Geometry) primaryFeature.getDefaultGeometry();
                Object primaryID = primaryFeature.getAttribute(uniqueField);

                // spatial query
                Filter filter = ff.intersects(ff.property(the_geom), ff.literal(primaryGeometry));
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature secondaryFeature = subIter.next();
                        Object secondaryID = secondaryFeature.getAttribute(uniqueField);
                        if (primaryID.equals(secondaryID)) {
                            continue;
                        }

                        Geometry secondaryGeom = (Geometry) secondaryFeature.getDefaultGeometry();
                        Geometry intersects = primaryGeometry.intersection(secondaryGeom);
                        if (intersects instanceof Point || intersects instanceof MultiPoint) {
                            swm.visit(primaryID, secondaryID);
                        }
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }
        return swm;
    }
}
