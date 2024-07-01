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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

/**
 * Creates a concave hull using the alpha shapes algorithm.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference https://github.com/qgis/QGIS/blob/master/python/plugins/processing/algs/qgis/ConcaveHull.py
 * 
 * @source $URL$
 * 
 */
public class ConcaveHullOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ConcaveHullOperation.class);

    final String GROUP_ALL = "all";

    final double DEFAULT_ALPHA = 0.3;

    public ConcaveHullOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, boolean removeHoles,
            boolean splitMultipart) throws IOException {
        return execute(features, DEFAULT_ALPHA, removeHoles, splitMultipart);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, double alpha,
            boolean removeHoles, boolean splitMultipart) throws IOException {
        return execute(features, null, alpha, removeHoles, splitMultipart);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression group,
            boolean removeHoles, boolean splitMultipart) throws IOException {
        return execute(features, group, DEFAULT_ALPHA, removeHoles, splitMultipart);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression group,
            double alpha, boolean removeHoles, boolean splitMultipart) throws IOException {
        alpha = Math.max(0.0, Math.min(1.0, alpha));

        SimpleFeatureType inputSchema = features.getSchema();
        CoordinateReferenceSystem crs = inputSchema.getCoordinateReferenceSystem();
        SimpleFeatureType schema = FeatureTypes.getDefaultType("concavehull", Polygon.class, crs);
        schema = FeatureTypes.add(schema, "cid", Integer.class);
        schema = FeatureTypes.add(schema, "group", String.class);

        // Gets the faces of the computed triangulation as a GeometryCollection of Polygon.
        Map<Object, CoordinateList> map = getCoordinateList(features, group);

        IFeatureInserter featureWriter = getFeatureWriter(schema);

        // Create features
        try {
            int cid = 0;
            for (Entry<Object, CoordinateList> entry : map.entrySet()) {
                String groupValue = Converters.convert(entry.getKey(), String.class);
                CoordinateList points = entry.getValue();

                List<Geometry> concaveHulls = null;
                try {
                    concaveHulls = generateConcaveHull(points, alpha, removeHoles, splitMultipart);
                } catch (NullPointerException npe) {
                    continue;
                }
                
                if(concaveHulls == null) {
                    continue;
                }

                // writer a feature
                for (int index = 0; index < concaveHulls.size(); index++) {
                    Geometry polygon = concaveHulls.get(index);

                    SimpleFeature feature = featureWriter.buildFeature();
                    feature.setDefaultGeometry(polygon);
                    feature.setAttribute("cid", ++cid);
                    feature.setAttribute("group", groupValue);
                    featureWriter.write(feature);
                }
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private List<Geometry> generateConcaveHull(List<Coordinate> coordinateList, double alpha,
            boolean removeHoles, boolean splitMultipart) {
        List<Geometry> concaveHulls = new ArrayList<Geometry>();

        DelaunayTriangulationBuilder vdBuilder = new DelaunayTriangulationBuilder();
        vdBuilder.setSites(coordinateList);
        vdBuilder.setTolerance(0.0d);
        Geometry triangles = vdBuilder.getTriangles(gf);

        // Get max edge length from Delaunay triangles
        List<Geometry> edges = new ArrayList<Geometry>();
        double maxLength = Double.MIN_VALUE;
        for (int index = 0; index < triangles.getNumGeometries(); index++) {
            Geometry triangle = triangles.getGeometryN(index);
            if (triangle == null || triangle.isEmpty()) {
                continue;
            }

            Coordinate[] coords = triangle.getCoordinates();
            Double maxSegDist = Double.MIN_VALUE;
            for (int i = 0; i < coords.length - 1; i++) {
                double dist = coords[i].distance(coords[i + 1]);

                maxSegDist = Math.max(maxSegDist, dist);
                maxLength = Math.max(maxLength, dist);
            }

            triangle.setUserData(maxSegDist); // set temporary distance
            edges.add(triangle);
        }

        // Remove geometries longer than cutoff
        final double cutoff = alpha * maxLength;
        for (int index = edges.size() - 1; index >= 0; index--) {
            Geometry triangle = edges.get(index);
            Double maxSegDist = Converters.convert(triangle.getUserData(), Double.class);
            if (maxSegDist > cutoff) {
                edges.remove(index);
            }
        }

        // Dissolve all Delaunay triangles
        CascadedPolygonUnion unionOp = new CascadedPolygonUnion(edges);
        Geometry unionGeometry = unionOp.union();

        boolean isMultipart = unionGeometry.getNumGeometries() > 1;

        if (isMultipart && splitMultipart) {
            for (int index = 0; index < unionGeometry.getNumGeometries(); index++) {
                Geometry polygon = unionGeometry.getGeometryN(index);
                if (polygon.isEmpty() || polygon.getArea() == 0) {
                    continue;
                }

                if (removeHoles) {
                    polygon = removeHoles((Polygon) polygon);
                }

                concaveHulls.add(polygon);
            }
        } else {
            Geometry polygon = unionGeometry.getGeometryN(0);
            if (removeHoles) {
                polygon = removeHoles((Polygon) polygon);
            }

            concaveHulls.add(polygon);
        }

        return concaveHulls;
    }

    private Geometry removeHoles(Polygon polygon) {
        GeometryFactory factory = polygon.getFactory();
        LineString exteriorRing = polygon.getExteriorRing();
        Geometry finalGeom = factory.createPolygon((LinearRing) exteriorRing, null);
        finalGeom.setUserData(polygon.getUserData());
        return finalGeom;
    }

    private Map<Object, CoordinateList> getCoordinateList(SimpleFeatureCollection inputFeatures,
            Expression group) {
        Map<Object, CoordinateList> map = new HashMap<Object, CoordinateList>();

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // use all coordinates
                Object key = group == null ? GROUP_ALL : group.evaluate(feature);

                CoordinateList coordinateList = map.get(key);
                if (coordinateList == null) {
                    coordinateList = new CoordinateList();
                    map.put(key, coordinateList);
                }

                coordinateList.add(geometry.getCoordinates(), false);
            }
        } finally {
            featureIter.close();
        }

        return map;
    }
}