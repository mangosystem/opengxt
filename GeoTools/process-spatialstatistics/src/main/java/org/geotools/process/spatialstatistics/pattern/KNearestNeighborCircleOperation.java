/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.process.spatialstatistics.pattern;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.measure.Measure;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import si.uom.SI;

/**
 * Creates a k-nearest neighbor circle from two features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KNearestNeighborCircleOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(KNearestNeighborCircleOperation.class);

    private static final String DIST_FIELD = "dist";

    private static final int DEFAULT_NEIGHBOR = 1;

    private GeodeticBuilder geodetic;

    public KNearestNeighborCircleOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures) throws IOException {
        return execute(inputFeatures, nearFeatures, DEFAULT_NEIGHBOR);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, int neighbor) throws IOException {
        return execute(inputFeatures, nearFeatures, neighbor, 0.0d, DistanceUnit.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, int neighbor, double maximumDistance,
            DistanceUnit distanceUnit) throws IOException {
        final int kNeighbor = neighbor <= 0 ? 1 : neighbor;

        // check coordinate reference system
        CoordinateReferenceSystem crsT = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = nearFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            nearFeatures = new ReprojectFeatureCollection(nearFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crsT);
        if (isGeographicCRS) {
            geodetic = new GeodeticBuilder(crsT);
            geodetic.setQuadrantSegments(24);
        }
        Unit<Length> targetUnit = UnitConverter.getLengthUnit(crsT);

        double maxDistance = maximumDistance == 0 ? Double.MAX_VALUE : maximumDistance;
        if (maximumDistance > 0 && maximumDistance != Double.MAX_VALUE
                && distanceUnit != DistanceUnit.Default) {
            // convert distance unit
            if (isGeographicCRS) {
                maxDistance = UnitConverter.convertDistance(maximumDistance, distanceUnit,
                        SI.METRE);
            } else {
                maxDistance = UnitConverter.convertDistance(maximumDistance, distanceUnit,
                        targetUnit);
            }
        }

        // create schema
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(inputFeatures.getSchema(), typeName,
                Polygon.class);
        schema = FeatureTypes.add(schema, DIST_FIELD, Double.class);

        // build feature
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            // build spatial index
            STRtree spatialIndex = buildSpatialIndex(nearFeatures);
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Geometry centroid = geometry.getCentroid();
                Coordinate coordinate = centroid.getCoordinate();

                // STRtree-nearestNeighbour: support GeoTools 20.x ~
                SpatialEvent start = new SpatialEvent(feature.getID(), coordinate);
                Object[] knns = spatialIndex.nearestNeighbour(new Envelope(coordinate), start,
                        new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                SpatialEvent s1 = (SpatialEvent) item1.getItem();
                                SpatialEvent s2 = (SpatialEvent) item2.getItem();

                                return s1.distance(s2);
                            }
                        }, kNeighbor);

                if (knns.length == 0) {
                    continue;
                }

                // find maximum radius
                double radius = Double.MIN_VALUE;
                for (Object object : knns) {
                    SpatialEvent nearest = (SpatialEvent) object;
                    double distance = start.distance(nearest);
                    if (distanceUnit != DistanceUnit.Default) {
                        if (isGeographicCRS) {
                            distance = geodetic.getDistance(coordinate, nearest.coordinate);
                            // meter to distance unit
                            distance = UnitConverter
                                    .convertDistance(new Measure(distance, SI.METRE), distanceUnit);
                        } else {
                            // convert to distance unit
                            distance = UnitConverter.convertDistance(
                                    new Measure(distance, targetUnit), distanceUnit);
                        }
                    }

                    if (distance > maxDistance) {
                        continue;
                    }

                    radius = Math.max(radius, distance);
                }

                if (radius <= 0) {
                    continue;
                }

                // build circle and & write feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, false);
                newFeature.setAttribute(DIST_FIELD, radius);

                Geometry circle = isGeographicCRS ? //
                        geodetic.buffer(centroid, radius) // geodetic buffer
                        : centroid.buffer(radius, 24); // geometry buffer

                newFeature.setDefaultGeometry(circle);

                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree buildSpatialIndex(SimpleFeatureCollection features) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate centroid = geometry.getCentroid().getCoordinate();

                SpatialEvent event = new SpatialEvent(feature.getID(), centroid);
                spatialIndex.insert(new Envelope(centroid), event);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }
}
