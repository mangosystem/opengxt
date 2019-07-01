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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.measure.Measure;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import si.uom.SI;

/**
 * Calculates distance and additional proximity information between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class NearOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(NearOperation.class);

    protected static final String DIST_FIELD = "dist";

    private GeodeticBuilder geodetic;

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField) throws IOException {
        return execute(inputFeatures, nearFeatures, nearIdField, Double.MAX_VALUE);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField, double maximumDistance)
            throws IOException {
        return execute(inputFeatures, nearFeatures, nearIdField, maximumDistance,
                DistanceUnit.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, String nearIdField, double maximumDistance,
            DistanceUnit distanceUnit) throws IOException {
        SimpleFeatureType inputSchema = inputFeatures.getSchema();

        SimpleFeatureType featureType = null;
        featureType = FeatureTypes.build(inputSchema, inputSchema.getTypeName());

        boolean hasID = nearIdField != null && nearFeatures.getSchema().indexOf(nearIdField) != -1;
        if (hasID) {
            AttributeDescriptor nearIdDesc = nearFeatures.getSchema().getDescriptor(nearIdField);
            featureType = FeatureTypes.add(featureType, nearIdDesc);
        }
        featureType = FeatureTypes.add(featureType, DIST_FIELD, Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

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

        STRtree spatialIndex = loadNearFeatures(nearFeatures, nearIdField);

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object id = hasID ? feature.getAttribute(nearIdField) : feature.getID();
                NearFeature source = new NearFeature(geometry, id);

                // find nearest hub
                NearFeature nearest = (NearFeature) spatialIndex.nearestNeighbour(
                        geometry.getEnvelopeInternal(), source, new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                NearFeature s1 = (NearFeature) item1.getItem();
                                NearFeature s2 = (NearFeature) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                double minumumDistance = source.location.distance(nearest.location);
                double distance = minumumDistance;
                if (distanceUnit != DistanceUnit.Default) {
                    if (isGeographicCRS) {
                        Coordinate[] points = DistanceOp.nearestPoints(geometry, nearest.location);
                        minumumDistance = geodetic.getDistance(points[0], points[1]);

                        // meter to distance unit
                        distance = UnitConverter.convertDistance(
                                new Measure(minumumDistance, SI.METRE), distanceUnit);
                    } else {
                        // convert to distance unit
                        distance = UnitConverter.convertDistance(new Measure(distance, targetUnit),
                                distanceUnit);
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                if (maxDistance < minumumDistance) {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, nearest.id);
                    }
                    newFeature.setAttribute(DIST_FIELD, null);
                } else {
                    if (hasID) {
                        newFeature.setAttribute(nearIdField, nearest.id);
                    }
                    newFeature.setAttribute(DIST_FIELD, distance);
                }

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree loadNearFeatures(SimpleFeatureCollection features, String idField) {
        STRtree spatialIndex = new STRtree();
        boolean hasID = idField != null && features.getSchema().indexOf(idField) != -1;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object id = hasID ? feature.getAttribute(idField) : feature.getID();
                NearFeature nearFeature = new NearFeature(geometry, id);
                spatialIndex.insert(geometry.getEnvelopeInternal(), nearFeature);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class NearFeature {

        public Geometry location;

        public Object id;

        public NearFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}