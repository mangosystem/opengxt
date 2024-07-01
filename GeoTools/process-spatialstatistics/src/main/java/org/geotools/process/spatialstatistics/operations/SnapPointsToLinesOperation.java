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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * Snaps each point in the point features to the closest point on the nearest line in the line features, provided it is within the user defined snap
 * tolerance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class SnapPointsToLinesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(SnapPointsToLinesOperation.class);

    public SnapPointsToLinesOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection pointFeatures,
            SimpleFeatureCollection lineFeatures, double tolerance) throws IOException {
        // check coordinate reference system
        CoordinateReferenceSystem crsT = pointFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = lineFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            lineFeatures = new ReprojectFeatureCollection(lineFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        STRtree spatialIndex = loadNearFeatures(lineFeatures);

        // prepare transactional feature store
        SimpleFeatureType featureType = pointFeatures.getSchema();
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = pointFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry point = (Geometry) feature.getDefaultGeometry();
                NearFeature source = new NearFeature(point, feature.getID());

                NearFeature nearest = null;
                if (tolerance == 0) {
                    // find nearest feature
                    nearest = (NearFeature) spatialIndex.nearestNeighbour(
                            point.getEnvelopeInternal(), source, new ItemDistance() {
                                @Override
                                public double distance(ItemBoundable item1, ItemBoundable item2) {
                                    NearFeature s1 = (NearFeature) item1.getItem();
                                    NearFeature s2 = (NearFeature) item2.getItem();
                                    return s1.location.distance(s2.location);
                                }
                            });
                } else {
                    // find nearest feature within search tolerance
                    Envelope searchEnv = point.getEnvelopeInternal();
                    searchEnv.expandBy(tolerance);

                    double minDistance = Double.MAX_VALUE;
                    for (@SuppressWarnings("unchecked")
                    Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(
                            searchEnv).iterator(); iter.hasNext();) {
                        NearFeature sample = iter.next();
                        double distance = point.distance(sample.location);
                        if (distance <= tolerance && minDistance > distance) {
                            minDistance = distance;
                            nearest = sample;
                        }
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                if (nearest != null) {
                    Coordinate[] coords = DistanceOp.nearestPoints(point, nearest.location);
                    Point snapped = point.getFactory().createPoint(coords[1]);
                    newFeature.setDefaultGeometry(snapped);
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

    private STRtree loadNearFeatures(SimpleFeatureCollection features) {
        STRtree spatialIndex = new STRtree();
        boolean isPolygon = FeatureTypes.getSimpleShapeType(features) == SimpleShapeType.POLYGON;
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (isPolygon) {
                    geometry = geometry.getBoundary();
                }
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

        public Object id;

        public NearFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}