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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * Simplifies polygon outlines by removing relatively extraneous vertices while preserving shape.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class SimplifyPolygonFeaturesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging
            .getLogger(SimplifyPolygonFeaturesOperation.class);

    private STRtree spatialIndex;

    public SimplifyPolygonFeaturesOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygonFeatures, double tolerance)
            throws IOException {
        return execute(polygonFeatures, tolerance, true);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygonFeatures,
            double tolerance, boolean preserveTopology) throws IOException {
        return execute(polygonFeatures, tolerance, preserveTopology, 0d);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygonFeatures,
            double tolerance, boolean preserveTopology, double minimumArea) throws IOException {

        // prepare spatial index
        buildSpatialIndex(polygonFeatures);

        // prepare transactional feature store
        SimpleFeatureType featureType = polygonFeatures.getSchema();
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = polygonFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multiPolygon = (Geometry) feature.getDefaultGeometry();
                GeometryFactory factory = multiPolygon.getFactory();

                List<Polygon> polygons = new ArrayList<Polygon>();
                for (int index = 0; index < multiPolygon.getNumGeometries(); index++) {
                    Polygon source = (Polygon) multiPolygon.getGeometryN(index);

                    if (minimumArea > source.getArea()) {
                        continue;
                    }

                    // 1. Exterior Ring
                    LinearRing exteriorRing = (LinearRing) source.getExteriorRing();
                    LinearRing shell = exteriorRing;

                    List<LineString> neighbors = searchNeighbors(feature.getID(), exteriorRing);
                    if (neighbors.size() == 0) {
                        polygons.add((Polygon) simplify(source, tolerance, preserveTopology));
                        continue;
                    }

                    List<Coordinate> intersections = getIntersectionPoints(exteriorRing, neighbors);
                    if (intersections.size() > 0) {
                        Polygonizer polygonizer = new Polygonizer();
                        List<Geometry> splits = splitLines(exteriorRing, intersections);
                        for (Geometry lineString : splits) {
                            if (lineString == null || lineString.getLength() == 0) {
                                continue;
                            }

                            // simplify geometries
                            Geometry simplified = simplify(lineString, tolerance, preserveTopology);
                            polygonizer.add(simplified);
                        }

                        @SuppressWarnings("unchecked")
                        List<Polygon> polygonized = (List<Polygon>) polygonizer.getPolygons();
                        if (polygonized.size() > 0) {
                            shell = (LinearRing) polygonized.get(0).getExteriorRing();
                        } else {
                            shell = (LinearRing) simplify(exteriorRing, tolerance, preserveTopology);
                        }
                    } else {
                        shell = (LinearRing) simplify(exteriorRing, tolerance, preserveTopology);
                    }

                    // 2. Interior Ring : skip intersection test
                    int numInteriorRing = source.getNumInteriorRing();
                    List<LinearRing> holes = new ArrayList<LinearRing>();
                    for (int idxRing = 0; idxRing < numInteriorRing; idxRing++) {
                        LineString ring = source.getInteriorRingN(idxRing);

                        LineString hole = (LineString) simplify(ring, tolerance, preserveTopology);
                        if (hole.getNumPoints() >= 4) {
                            holes.add(factory.createLinearRing(hole.getCoordinates()));
                        }
                    }

                    // 3. create Polygon
                    polygons.add(factory.createPolygon(shell,
                            GeometryFactory.toLinearRingArray(holes)));
                }

                // create & insert feature
                Geometry simplified = factory.createMultiPolygon(GeometryFactory
                        .toPolygonArray(polygons));
                insertFeature(featureWriter, feature, simplified);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private List<Coordinate> getIntersectionPoints(LineString source, List<LineString> neighbors) {
        final List<Point> points = new ArrayList<Point>();
        final List<LineString> lines = new ArrayList<LineString>();

        // intersection
        for (LineString neighbor : neighbors) {
            lines.clear();
            Geometry intersections = source.intersection(neighbor);

            // first, extract points
            intersections.apply(new GeometryComponentFilter() {
                @Override
                public void filter(Geometry geom) {
                    if (geom instanceof Point) {
                        points.add((Point) geom);
                    } else if (geom instanceof MultiPoint) {
                        for (int index = 0; index < geom.getNumGeometries(); index++) {
                            points.add((Point) geom.getGeometryN(index));
                        }
                    }
                }
            });

            // second, extract line
            LineMerger lineMerger = new LineMerger();
            lineMerger.add(intersections);

            @SuppressWarnings("rawtypes")
            Collection collection = lineMerger.getMergedLineStrings();

            @SuppressWarnings("rawtypes")
            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                lines.add((LineString) iter.next());
            }

            // extract node
            if (lines.size() > 0) {
                for (LineString line : lines) {
                    points.add(line.getStartPoint());
                    points.add(line.getEndPoint());
                }
            }
        }

        Geometry multi = source.getFactory().createMultiPoint(GeometryFactory.toPointArray(points));
        return Arrays.asList(multi.union().getCoordinates());
    }

    private List<Geometry> splitLines(LineString line, List<Coordinate> coordinates) {
        List<Geometry> splits = new ArrayList<Geometry>();
        boolean needMerge = coordinates.contains(line.getCoordinate()) == false;

        LocationIndexedLine liLine = new LocationIndexedLine(line);

        // sort point along line
        SortedMap<Double, LinearLocation> sortedMap = new TreeMap<Double, LinearLocation>();
        for (Coordinate coordinate : coordinates) {
            LinearLocation location = liLine.indexOf(coordinate);
            int segIndex = location.getSegmentIndex();
            double segFraction = location.getSegmentFraction();
            sortedMap.put(Double.valueOf(segIndex + segFraction), location);
        }

        // split
        LinearLocation startIndex = liLine.getStartIndex();
        for (Entry<Double, LinearLocation> entrySet : sortedMap.entrySet()) {
            LinearLocation endIndex = entrySet.getValue();
            LineString left = (LineString) liLine.extractLine(startIndex, endIndex);
            if (left != null && !left.isEmpty() && left.getLength() > 0) {
                left.setUserData(entrySet.getValue());
                splits.add(left);
            }
            startIndex = endIndex;
        }

        // add last segment
        Geometry left = liLine.extractLine(startIndex, liLine.getEndIndex());
        if (left != null && !left.isEmpty() && left.getLength() > 0) {
            splits.add(left);
        }

        if (needMerge && splits.size() > 1) {
            LineMerger lineMerger = new LineMerger();
            lineMerger.add(splits.get(0));
            lineMerger.add(splits.get(splits.size() - 1));

            @SuppressWarnings("rawtypes")
            Collection collection = lineMerger.getMergedLineStrings();
            Geometry merged = (Geometry) collection.iterator().next();

            splits.remove(0);
            splits.remove(splits.size() - 1);
            splits.add(merged);
        }

        return splits;
    }

    private Geometry simplify(Geometry source, double tolerance, boolean preserveTopology) {
        if (preserveTopology) {
            return TopologyPreservingSimplifier.simplify(source, tolerance);
        } else {
            return DouglasPeuckerSimplifier.simplify(source, tolerance);
        }
    }

    private List<LineString> searchNeighbors(String id, Geometry boundary) {
        List<LineString> neighbors = new ArrayList<LineString>();
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(boundary);

        for (@SuppressWarnings("unchecked")
        Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(
                boundary.getEnvelopeInternal()).iterator(); iter.hasNext();) {
            NearFeature sample = iter.next();
            if (sample.id.equals(id) || prepared.disjoint(sample.location)) {
                continue;
            }
            neighbors.add(sample.location);
        }

        return neighbors;
    }

    private void insertFeature(IFeatureInserter featureWriter, SimpleFeature source,
            Geometry newGeometry) throws IOException {
        SimpleFeature newFeature = featureWriter.buildFeature();
        featureWriter.copyAttributes(source, newFeature, false);
        newFeature.setDefaultGeometry(newGeometry);
        featureWriter.write(newFeature);
    }

    private void buildSpatialIndex(SimpleFeatureCollection features) {
        spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometry = geometry.getBoundary(); // Convert to (Multi)LineString

                for (int index = 0; index < geometry.getNumGeometries(); index++) {
                    LineString part = (LineString) geometry.getGeometryN(index);
                    NearFeature nearFeature = new NearFeature(part, feature.getID());
                    spatialIndex.insert(part.getEnvelopeInternal(), nearFeature);
                }
            }
        } finally {
            featureIter.close();
        }
    }

    static final class NearFeature {

        public LineString location;

        public String id;

        public NearFeature(LineString location, String id) {
            this.location = location;
            this.id = id;
        }
    }
}