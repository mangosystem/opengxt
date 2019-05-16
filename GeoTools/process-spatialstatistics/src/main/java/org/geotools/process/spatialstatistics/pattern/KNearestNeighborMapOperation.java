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
package org.geotools.process.spatialstatistics.pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.KnnSearch;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * K-Nearest Neighbor Map - Spatial Clustering.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KNearestNeighborMapOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(KNearestNeighborMapOperation.class);

    static String[] FIELDS = { "orig", "dest", "distance", "group" };

    private int featureCount = 0;

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, int neighbor,
            boolean convexHull) throws IOException {
        // build spatial index
        STRtree spatialIndex = buildSpatialIndex(features);
        List<Coordinate> coordinates = new ArrayList<Coordinate>();

        // create schema
        String typeName = features.getSchema().getTypeName();
        CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(typeName, LineString.class, crs);

        int length = typeName.length() + String.valueOf(featureCount).length() + 2;
        schema = FeatureTypes.add(schema, FIELDS[0], String.class, length);
        schema = FeatureTypes.add(schema, FIELDS[1], String.class, length);

        schema = FeatureTypes.add(schema, FIELDS[2], Double.class, 38);
        schema = FeatureTypes.add(schema, FIELDS[3], String.class, 20);

        // build feature
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = features.features();
        try {
            KnnSearch knnSearch = new KnnSearch(spatialIndex);
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();
                if (convexHull) {
                    coordinates.add(coordinate);
                }

                SpatialEvent start = new SpatialEvent(feature.getID(), coordinate);
                Object[] knns = knnSearch.kNearestNeighbour(new Envelope(coordinate), start,
                        new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                SpatialEvent s1 = (SpatialEvent) item1.getItem();
                                SpatialEvent s2 = (SpatialEvent) item2.getItem();
                                if (s1.id.equals(s2.id)) {
                                    return Double.MAX_VALUE;
                                }
                                return s1.distance(s2);
                            }
                        }, neighbor);

                // build line & write feature
                for (Object object : knns) {
                    SpatialEvent nearest = (SpatialEvent) object;

                    Geometry line = createLineString(start, nearest);
                    double distance = line.getLength();
                    if (distance == 0) {
                        continue;
                    }

                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setDefaultGeometry(line);
                    newFeature.setAttribute(FIELDS[0], start.id);
                    newFeature.setAttribute(FIELDS[1], nearest.id);
                    newFeature.setAttribute(FIELDS[2], distance);
                    newFeature.setAttribute(FIELDS[3], "Nearest");
                    featureWriter.write(newFeature);
                }
            }

            // finally convexhull
            if (convexHull) {
                Coordinate[] coords = CoordinateArrays.toCoordinateArray(coordinates);
                ConvexHull cvxBuidler = new ConvexHull(coords, new GeometryFactory());
                Geometry convexHullGeom = cvxBuidler.getConvexHull();

                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setDefaultGeometry(convexHullGeom.getBoundary());
                newFeature.setAttribute(FIELDS[3], "ConvexHull");
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private Geometry createLineString(SpatialEvent start, SpatialEvent end) {
        return gf.createLineString(new Coordinate[] { start.getCoordinate(), end.getCoordinate() });
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
                featureCount++;
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }
}
