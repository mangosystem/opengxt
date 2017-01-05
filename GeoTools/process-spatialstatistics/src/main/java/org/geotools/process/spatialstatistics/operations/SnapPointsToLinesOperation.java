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
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;

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
        SimpleFeatureType featureType = pointFeatures.getSchema();

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        STRtree spatialIndex = loadNearFeatures(lineFeatures);
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