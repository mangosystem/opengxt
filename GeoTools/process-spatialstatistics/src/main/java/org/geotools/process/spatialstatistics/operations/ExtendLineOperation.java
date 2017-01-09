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
import java.util.Iterator;
import java.util.List;
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
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Extends line segments to the first intersecting feature within a specified distance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class ExtendLineOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ExtendLineOperation.class);

    private STRtree spatialIndex;

    private double length = 0d;

    private boolean extendTo = true;

    public ExtendLineOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection lineFeatures, double length,
            boolean extendTo) throws IOException {
        this.length = length;
        this.extendTo = extendTo;

        if (length <= 0) {
            return lineFeatures;
        }

        // prepare spatial index
        buildSpatialIndex(lineFeatures);

        // prepare transactional feature store
        SimpleFeatureType featureType = lineFeatures.getSchema();
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = lineFeatures.features();
        try {
            List<LineString> segments = new ArrayList<LineString>();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // extend
                segments.clear();
                Geometry multiPart = (Geometry) feature.getDefaultGeometry();
                for (int index = 0; index < multiPart.getNumGeometries(); index++) {
                    LineString part = (LineString) multiPart.getGeometryN(index);
                    LineString segment = processLineString(feature.getID(), part);
                    if (segment != null) {
                        segments.add(segment);
                    }
                }

                // build geometry
                if (segments.size() == 0) {
                    continue;
                }

                LineString[] lsArray = GeometryFactory.toLineStringArray(segments);
                Geometry trimmed = multiPart.getFactory().createMultiLineString(lsArray);
                if (trimmed == null || trimmed.isEmpty()) {
                    continue;
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, false);
                newFeature.setDefaultGeometry(trimmed);
                featureWriter.write(newFeature);
                segments.clear();
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private LineString processLineString(String id, LineString input) {
        LineString part = exendLine(input, length, length);
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(part);
        GeometryFactory factory = part.getFactory();

        List<LineString> lineStrings = new ArrayList<LineString>();
        for (@SuppressWarnings("unchecked")
        Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(
                part.getEnvelopeInternal()).iterator(); iter.hasNext();) {
            NearFeature sample = iter.next();
            if (sample.id.equals(id) || prepared.disjoint(sample.location)) {
                continue;
            }
            lineStrings.add(sample.location);
        }

        // isolated line
        if (lineStrings.size() == 0) {
            return input;
        }

        // post process
        LineString[] lsArray = GeometryFactory.toLineStringArray(lineStrings);
        MultiLineString multi = factory.createMultiLineString(lsArray);
        List<Point> intersections = extractPoints(part.intersection(multi));

        final double[] minDist = { Double.MAX_VALUE, Double.MAX_VALUE };
        final Point[] minPoint = { null, null };
        for (Point source : intersections) {
            double startDist = source.distance(part.getStartPoint());
            if (startDist < minDist[0]) {
                minDist[0] = startDist;
                minPoint[0] = source;
            }

            double endDist = source.distance(part.getEndPoint());
            if (endDist < minDist[1]) {
                minDist[1] = endDist;
                minPoint[1] = source;
            }
        }

        // connected line
        if (minDist[0] == 0d && minDist[1] == 0d) {
            return input;
        }

        // build trimmed LineString
        Coordinate[] coordinates = input.getCoordinates();
        if (minDist[0] > 0 && minDist[0] <= length) {
            coordinates[0] = minPoint[0].getCoordinate();
        }

        if (minDist[1] > 0 && minDist[1] <= length) {
            coordinates[coordinates.length - 1] = minPoint[1].getCoordinate();
        }

        return factory.createLineString(coordinates);
    }

    private List<Point> extractPoints(Geometry intersections) {
        final List<Point> points = new ArrayList<Point>();
        intersections.apply(new GeometryComponentFilter() {
            @Override
            public void filter(Geometry geom) {
                if (geom instanceof Point) {
                    points.add((Point) geom);
                }
            }
        });
        return points;
    }

    private void buildSpatialIndex(SimpleFeatureCollection features) {
        spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multiPart = (Geometry) feature.getDefaultGeometry();
                for (int index = 0; index < multiPart.getNumGeometries(); index++) {
                    LineString part = (LineString) multiPart.getGeometryN(index);
                    if (extendTo) {
                        part = exendLine(part, length, length);
                    }
                    NearFeature nearFeature = new NearFeature(part, feature.getID());
                    spatialIndex.insert(part.getEnvelopeInternal(), nearFeature);
                }
            }
        } finally {
            featureIter.close();
        }
    }

    private LineString exendLine(LineString segment, double fromOffset, double toOffset) {
        Coordinate[] coordinates = ((LineString) segment.clone()).getCoordinates();

        LineSegment line = new LineSegment();
        if (fromOffset > 0) {
            line.p0 = coordinates[0];
            line.p1 = coordinates[1];
            coordinates[0] = offset(line.p0, line.angle(), -fromOffset);
        }

        if (toOffset > 0) {
            line.p0 = coordinates[coordinates.length - 2];
            line.p1 = coordinates[coordinates.length - 1];
            coordinates[coordinates.length - 1] = offset(line.p1, line.angle(), toOffset);
        }

        return segment.getFactory().createLineString(coordinates);
    }

    private Coordinate offset(Coordinate coordinate, double angle, double distance) {
        double newX = coordinate.x + distance * Math.cos(angle);
        double newY = coordinate.y + distance * Math.sin(angle);
        return new Coordinate(newX, newY);
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