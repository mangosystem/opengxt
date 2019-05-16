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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by nearest distance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class HubLinesByDistanceOperation extends AbstractHubLinesOperation {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByDistanceOperation.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures, boolean useCentroid, boolean preserveAttributes,
            double maximumDistance) throws IOException {

        this.setPreserveAttributes(preserveAttributes);
        this.setMaximumDistance(maximumDistance);
        this.setUseCentroid(useCentroid);

        return execute(hubFeatures, hubIdField, spokeFeatures);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures) throws IOException {
        SimpleFeatureType spokeSchema = spokeFeatures.getSchema();
        CoordinateReferenceSystem crs = spokeSchema.getCoordinateReferenceSystem();

        SimpleFeatureType featureType = null;
        if (preserveAttributes) {
            featureType = FeatureTypes.build(spokeSchema, TYPE_NAME, LineString.class);
        } else {
            String geomName = spokeSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName, LineString.class, crs);
        }

        boolean hasHubID = hubIdField != null && hubFeatures.getSchema().indexOf(hubIdField) != -1;
        if (hasHubID) {
            AttributeDescriptor hubIdDesc = hubFeatures.getSchema().getDescriptor(hubIdField);
            featureType = FeatureTypes.add(featureType, hubIdDesc);
        }
        featureType = FeatureTypes.add(featureType, HUB_DIST, Double.class, 38);

        // check coordinate reference system
        CoordinateReferenceSystem crsT = spokeFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = hubFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            hubFeatures = new ReprojectFeatureCollection(hubFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // build spatial index
        STRtree spatialIndex = loadHubs(hubFeatures, hubIdField);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator spokeIter = spokeFeatures.features();
        try {
            while (spokeIter.hasNext()) {
                SimpleFeature feature = spokeIter.next();
                Geometry spokeGeom = (Geometry) feature.getDefaultGeometry();
                Object id = hasHubID ? feature.getAttribute(hubIdField) : feature.getID();
                if (useCentroid) {
                    spokeGeom = getCentroid(spokeGeom);
                }

                // find nearest hub
                Hub nearest = (Hub) spatialIndex.nearestNeighbour(spokeGeom.getEnvelopeInternal(),
                        new Hub(spokeGeom, id), new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                Hub s1 = (Hub) item1.getItem();
                                Hub s2 = (Hub) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                // create line: direction = hub --> spoke
                Geometry hubLine = getShortestLine(nearest.location, spokeGeom, false);
                double distance = hubLine.getLength();
                if (distance == 0 || this.maximumDistance < distance) {
                    continue;
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                if (preserveAttributes) {
                    featureWriter.copyAttributes(feature, newFeature, false);
                }

                newFeature.setDefaultGeometry(hubLine);
                if (hasHubID) {
                    newFeature.setAttribute(hubIdField, nearest.id);
                }
                newFeature.setAttribute(HUB_DIST, distance);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(spokeIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree loadHubs(SimpleFeatureCollection features, String idField) {
        STRtree spatialIndex = new STRtree();
        boolean hasID = idField != null && features.getSchema().indexOf(idField) != -1;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (useCentroid) {
                    geometry = getCentroid(geometry);
                }

                Object id = hasID ? feature.getAttribute(idField) : feature.getID();
                Hub hub = new Hub(geometry, id);
                spatialIndex.insert(geometry.getEnvelopeInternal(), hub);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    private Geometry getCentroid(Geometry input) {
        Class<?> geomBinding = input.getClass();

        if (MultiPolygon.class.equals(geomBinding)) {
            if (input.getNumGeometries() == 1) {
                return getCentroid(input.getGeometryN(0));
            }

            // largest area
            return getCentroid(getLargestGeometry(input, false));
        } else if (Polygon.class.equals(geomBinding)) {
            // point in polygon
            Point center = input.getCentroid();
            if (!input.contains(center)) {
                center = input.getInteriorPoint();
            }
            return center;
        } else if (MultiLineString.class.equals(geomBinding)) {
            if (input.getNumGeometries() == 1) {
                return getCentroid(input.getGeometryN(0));
            }

            // largest length
            return getCentroid(getLargestGeometry(input, true));
        } else if (LineString.class.equals(geomBinding)) {
            // point on linestring
            LengthIndexedLine lil = new LengthIndexedLine(input);
            double index = input.getLength() * 0.5;
            Coordinate midpoint = lil.extractPoint(index);
            return input.getFactory().createPoint(midpoint);
        } else if (MultiPoint.class.equals(geomBinding)) {
            // central point
            return getCentralPoint((MultiPoint) input);
        } else if (Point.class.equals(geomBinding)) {
            return input;
        }

        return input.getCentroid();
    }

    private Geometry getLargestGeometry(Geometry multi, boolean useLength) {
        Geometry largest = null;

        double max = Double.MIN_VALUE;
        for (int i = 0; i < multi.getNumGeometries(); i++) {
            Geometry part = multi.getGeometryN(i);
            double cur = useLength ? part.getLength() : part.getArea();

            if (max < cur) {
                max = cur;
                largest = part;
            }
        }

        return largest;
    }

    private Point getCentralPoint(MultiPoint multiPoint) {
        int numPoints = multiPoint.getNumPoints();
        if (numPoints == 1) {
            return (Point) multiPoint.getGeometryN(0);
        }

        double minDistance = Double.MAX_VALUE;
        Point centralPoint = null;

        for (int i = 0; i < numPoints; i++) {
            Point ce = (Point) multiPoint.getGeometryN(i);
            double curDistance = 0d;

            for (int j = 0; j < numPoints; j++) {
                Point de = (Point) multiPoint.getGeometryN(j);
                if (i != j) {
                    curDistance += ce.distance(de);
                }
            }

            if (minDistance > curDistance) {
                minDistance = curDistance;
                centralPoint = ce;
            }
        }

        return centralPoint;
    }

    static final class Hub {

        public Geometry location;

        public Object id;

        public Hub(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}