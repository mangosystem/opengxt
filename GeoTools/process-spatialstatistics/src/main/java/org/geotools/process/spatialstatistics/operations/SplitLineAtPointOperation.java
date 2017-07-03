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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Splits line features based on intersection or proximity to point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class SplitLineAtPointOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(SplitLineAtPointOperation.class);

    public SplitLineAtPointOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection lineFeatures,
            SimpleFeatureCollection pointFeatures, double tolerance) throws IOException {
        // check coordinate reference system
        CoordinateReferenceSystem crsT = lineFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = pointFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            pointFeatures = new ReprojectFeatureCollection(pointFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        STRtree spatialIndex = loadNearFeatures(pointFeatures);

        // prepare transactional feature store
        SimpleFeatureType featureType = lineFeatures.getSchema();
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = lineFeatures.features();
        try {
            boolean isZeroTolerance = tolerance == 0;
            List<Coordinate> coordinates = new ArrayList<Coordinate>();

            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry line = (Geometry) feature.getDefaultGeometry();
                NearFeature source = new NearFeature(line, feature.getID());

                coordinates.clear();

                // first, find all point features within search tolerance
                Envelope searchEnv = line.getEnvelopeInternal();
                if (!isZeroTolerance) {
                    searchEnv.expandBy(tolerance);
                }

                for (@SuppressWarnings("unchecked")
                Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(searchEnv)
                        .iterator(); iter.hasNext();) {
                    NearFeature sample = iter.next();
                    double distance = line.distance(sample.location);

                    if (isZeroTolerance) {
                        if (SSUtils.compareDouble(distance, tolerance)) {
                            coordinates.add(sample.location.getCoordinate());
                        }
                    } else {
                        if (distance <= tolerance) {
                            coordinates.add(sample.location.getCoordinate());
                        }
                    }
                }

                // find nearest point feature
                if (isZeroTolerance && coordinates.size() == 0) {
                    NearFeature nearest = (NearFeature) spatialIndex.nearestNeighbour(
                            line.getEnvelopeInternal(), source, new ItemDistance() {
                                @Override
                                public double distance(ItemBoundable item1, ItemBoundable item2) {
                                    NearFeature s1 = (NearFeature) item1.getItem();
                                    NearFeature s2 = (NearFeature) item2.getItem();
                                    return s1.location.distance(s2.location);
                                }
                            });
                    coordinates.add(nearest.location.getCoordinate());
                }

                // create & insert feature
                if (coordinates.size() > 0) {
                    List<Geometry> splits = splitLines(line, coordinates);
                    for (Geometry lineString : splits) {
                        SimpleFeature newFeature = featureWriter.buildFeature();
                        featureWriter.copyAttributes(feature, newFeature, false);
                        newFeature.setDefaultGeometry(lineString);
                        featureWriter.write(newFeature);
                    }
                } else {
                    // insert source feature
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    featureWriter.copyAttributes(feature, newFeature, true);
                    featureWriter.write(newFeature);
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private List<Geometry> splitLines(Geometry line, List<Coordinate> coordinates) {
        List<Geometry> splits = new ArrayList<Geometry>();

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
                left.setUserData(line.getUserData());
                splits.add(left);
            }
            startIndex = endIndex;
        }

        // add last segment
        Geometry left = liLine.extractLine(startIndex, liLine.getEndIndex());
        if (left != null && !left.isEmpty() && left.getLength() > 0) {
            left.setUserData(line.getUserData());
            splits.add(left);
        }

        return splits;
    }

    private STRtree loadNearFeatures(SimpleFeatureCollection features) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                for (int index = 0; index < geometry.getNumGeometries(); index++) {
                    Geometry part = geometry.getGeometryN(index);
                    NearFeature nearFeature = new NearFeature(part, feature.getID());
                    spatialIndex.insert(part.getEnvelopeInternal(), nearFeature);
                }
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