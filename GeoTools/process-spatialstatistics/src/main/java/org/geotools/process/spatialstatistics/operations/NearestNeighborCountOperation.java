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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;

import si.uom.SI;

/**
 * Calculates count between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class NearestNeighborCountOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(NearestNeighborCountOperation.class);

    private static final String COUNT_FIELD = "count";

    private double searchRadius = Double.MAX_VALUE;

    private DistanceUnit radiusUnit = DistanceUnit.Default;

    private boolean isGeographicCRS = false;

    private GeodeticBuilder deodetic;

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String countField,
            SimpleFeatureCollection nearFeatures) throws IOException {
        return execute(inputFeatures, countField, nearFeatures, Double.MAX_VALUE,
                DistanceUnit.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String countField,
            SimpleFeatureCollection nearFeatures, double searchRadius) throws IOException {
        return execute(inputFeatures, countField, nearFeatures, searchRadius, DistanceUnit.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String countField,
            SimpleFeatureCollection nearFeatures, double searchRadius, DistanceUnit radiusUnit)
            throws IOException {
        if (countField == null || countField.isEmpty()) {
            countField = COUNT_FIELD;
        }

        // check coordinate reference system
        CoordinateReferenceSystem crsT = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = nearFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            nearFeatures = new ReprojectFeatureCollection(nearFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        this.radiusUnit = radiusUnit;
        this.isGeographicCRS = UnitConverter.isGeographicCRS(crsT);
        if (isGeographicCRS) {
            deodetic = new GeodeticBuilder(crsT);
        }
        Unit<Length> targetUnit = UnitConverter.getLengthUnit(crsT);

        if (Double.isNaN(searchRadius) || Double.isInfinite(searchRadius) || searchRadius <= 0
                || searchRadius == Double.MAX_VALUE) {
            this.searchRadius = Double.MAX_VALUE;
        } else {
            // convert distance unit
            this.searchRadius = searchRadius;
            if (radiusUnit != DistanceUnit.Default) {
                if (isGeographicCRS) {
                    this.searchRadius = UnitConverter.convertDistance(searchRadius, radiusUnit,
                            SI.METRE);
                } else {
                    this.searchRadius = UnitConverter.convertDistance(searchRadius, radiusUnit,
                            targetUnit);
                }
            }
        }

        // 1. pre calculate
        Map<String, Integer> map = calculateNearest(inputFeatures, nearFeatures);

        // 2. write features
        SimpleFeatureType featureType = null;
        featureType = FeatureTypes.add(inputFeatures.getSchema(), countField, Integer.class, 6);

        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                Integer count = map.get(feature.getID());
                if (count == null) {
                    count = Integer.valueOf(0);
                }

                newFeature.setAttribute(countField, count);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private Map<String, Integer> calculateNearest(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        STRtree spatialIndex = loadNearFeatures(inputFeatures);

        SimpleFeatureIterator nearIter = nearFeatures.features();
        try {
            while (nearIter.hasNext()) {
                SimpleFeature feature = nearIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // find nearest feature
                NearFeature nearest = (NearFeature) spatialIndex.nearestNeighbour(
                        geometry.getEnvelopeInternal(), new NearFeature(geometry, feature.getID()),
                        new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                NearFeature s1 = (NearFeature) item1.getItem();
                                NearFeature s2 = (NearFeature) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                double nearestDistance = geometry.distance(nearest.location);
                if (radiusUnit != DistanceUnit.Default) {
                    if (isGeographicCRS) {
                        Coordinate[] points = DistanceOp.nearestPoints(geometry, nearest.location);
                        nearestDistance = deodetic.getDistance(points[0], points[1]);
                    }
                }

                if (nearestDistance > searchRadius) {
                    continue;
                }

                // update count
                Integer count = map.get(nearest.id);
                if (count == null) {
                    map.put(nearest.id, Integer.valueOf(1));
                } else {
                    map.put(nearest.id, Integer.valueOf(count + 1));
                }
            }
        } finally {
            nearIter.close();
        }
        return map;
    }

    private STRtree loadNearFeatures(SimpleFeatureCollection features) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                NearFeature nearFeature = new NearFeature(geometry, feature.getID());
                spatialIndex.insert(geometry.getEnvelopeInternal(), nearFeature);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class NearFeature {

        public Geometry location;

        public String id;

        public NearFeature(Geometry location, String id) {
            this.location = location;
            this.id = id;
        }
    }
}