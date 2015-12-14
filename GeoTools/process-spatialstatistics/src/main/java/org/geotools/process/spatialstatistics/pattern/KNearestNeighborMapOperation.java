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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DistanceFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

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

    private Geometry getConvexHull(List<SpatialEvent> events) {
        Coordinate[] coordinates = new Coordinate[events.size()];
        for (int k = 0; k < events.size(); k++) {
            SpatialEvent event = events.get(k);
            coordinates[k] = new Coordinate(event.x, event.y);
        }

        ConvexHull cvxBuidler = new ConvexHull(coordinates, new GeometryFactory());
        return cvxBuidler.getConvexHull();
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, int neighbor,
            boolean convexHull) throws IOException {
        CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType("knearest", LineString.class, crs);
        schema = FeatureTypes.add(schema, FIELDS[0], Integer.class, 19);
        schema = FeatureTypes.add(schema, FIELDS[1], Integer.class, 19);
        schema = FeatureTypes.add(schema, FIELDS[2], Double.class, 38);
        schema = FeatureTypes.add(schema, FIELDS[3], String.class, 20);

        // 1. pre calculation
        List<SpatialEvent> events = DistanceFactory.loadEvents(features, null);

        // 2. build feature
        int featureID = 1;
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            // k nearest neighbor = neighbor
            SortedMap<Double, SpatialEvent> map = new TreeMap<Double, SpatialEvent>();
            for (SpatialEvent start : events) {
                map.clear();
                for (SpatialEvent end : events) {
                    if (end.oid == start.oid) {
                        continue;
                    }

                    double currentDist = start.getDistance(end);
                    if (map.size() < neighbor) {
                        map.put(currentDist, end);
                    } else {
                        if (map.lastKey() > currentDist) {
                            map.put(currentDist, end);
                            map.remove(map.lastKey());
                        }
                    }
                }

                // build line
                for (SpatialEvent nearest : map.values()) {
                    Geometry line = gf.createLineString(new Coordinate[] { start.getCoordinate(),
                            nearest.getCoordinate() });
                    double distance = line.getLength();
                    if (distance == 0) {
                        continue;
                    }
                    SimpleFeature newFeature = featureWriter.buildFeature(Integer
                            .toString(featureID++));
                    newFeature.setDefaultGeometry(line);
                    newFeature.setAttribute(FIELDS[0], start.oid);
                    newFeature.setAttribute(FIELDS[1], nearest.oid);
                    newFeature.setAttribute(FIELDS[2], distance);
                    newFeature.setAttribute(FIELDS[3], "Nearest");
                    featureWriter.write(newFeature);
                }
            }

            // convexhull
            if (convexHull) {
                Geometry convexHullGeom = getConvexHull(events);
                SimpleFeature newFeature = featureWriter
                        .buildFeature(Integer.toString(featureID++));
                newFeature.setDefaultGeometry(convexHullGeom.getBoundary());
                newFeature.setAttribute(FIELDS[3], "ConvexHull");
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

}
