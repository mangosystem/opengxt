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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Removes portions of a line that extend a specified distance past a line intersection (dangles).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class TrimLineOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(TrimLineOperation.class);

    private STRtree spatialIndex;

    private double dangleLength = 0d;

    private boolean deleteShort = true;

    public TrimLineOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection lineFeatures,
            double dangleLength, boolean deleteShort) throws IOException {
        this.dangleLength = dangleLength;
        this.deleteShort = deleteShort;

        if (dangleLength <= 0) {
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

                // trim
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

    private LineString processLineString(String id, LineString part) {
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
            if (deleteShort && part.getLength() <= dangleLength) {
                return null;
            }
            return part;
        }

        // post process
        LineString[] lsArray = GeometryFactory.toLineStringArray(lineStrings);
        MultiLineString multi = factory.createMultiLineString(lsArray);
        List<Point> intersections = extractPoints(part.intersection(multi));

        final double[] minDist = { Double.MAX_VALUE, Double.MAX_VALUE };
        final Point[] minPoint = { null, null };
        for (Point source : intersections) {
            double startDist = source.distance(part.getStartPoint());
            double endDist = source.distance(part.getEndPoint());
            if (startDist < endDist) {
                if (startDist < minDist[0]) {
                    minDist[0] = startDist;
                    minPoint[0] = source;
                }
            } else {
                if (endDist < minDist[1]) {
                    minDist[1] = endDist;
                    minPoint[1] = source;
                }
            }
        }

        // connected line
        if (minDist[0] == 0d && minDist[1] == 0d) {
            return part;
        }

        // build trimmed LineString
        Coordinate[] coordinates = part.getCoordinates();
        if (minDist[0] > 0 && minDist[0] <= dangleLength) {
            coordinates[0] = minPoint[0].getCoordinate();
        }

        if (minDist[1] > 0 && minDist[1] <= dangleLength) {
            coordinates[coordinates.length - 1] = minPoint[1].getCoordinate();
        }

        // check deleteShort
        LineString trimmed = factory.createLineString(coordinates);
        if (deleteShort && trimmed.getLength() <= dangleLength) {
            return null;
        }

        return trimmed;
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