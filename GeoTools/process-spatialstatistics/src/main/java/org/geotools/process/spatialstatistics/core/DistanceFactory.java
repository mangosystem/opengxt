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
package org.geotools.process.spatialstatistics.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Help class for distance calculation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DistanceFactory {
    protected static final Logger LOGGER = Logging.getLogger(DistanceFactory.class);

    static GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    private DistanceMethod distanceType = DistanceMethod.Euclidean;

    public DistanceMethod getDistanceType() {
        return distanceType;
    }

    public void setDistanceType(DistanceMethod distanceType) {
        this.distanceType = distanceType;
    }

    public static DistanceFactory newInstance() {
        return new DistanceFactory();
    }

    public double getMeanDistance(List<SpatialEvent> spatialEventSet, SpatialEvent curEvent) {
        double sumDistance = 0.0;
        for (SpatialEvent destEvent : spatialEventSet) {
            if (destEvent.id.equals(curEvent.id)) {
                continue;
            }
            sumDistance += getDistance(curEvent, destEvent, distanceType);
        }
        return sumDistance / spatialEventSet.size();
    }

    public double getThresholDistance(SimpleFeatureCollection features) {
        // build spatial index
        final List<SpatialEvent> events = new ArrayList<SpatialEvent>();
        final STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate centroid = geometry.getCentroid().getCoordinate();

                SpatialEvent event = new SpatialEvent(feature.getID(), centroid);
                events.add(event);
                spatialIndex.insert(new Envelope(centroid), event);
            }
        } finally {
            featureIter.close();
        }

        // calculate nearest neighbor index
        double threshold = Double.MIN_VALUE;
        for (SpatialEvent source : events) {
            SpatialEvent nearest = (SpatialEvent) spatialIndex
                    .nearestNeighbour(new Envelope(source.coordinate), source, new ItemDistance() {
                        @Override
                        public double distance(ItemBoundable item1, ItemBoundable item2) {
                            SpatialEvent s1 = (SpatialEvent) item1.getItem();
                            SpatialEvent s2 = (SpatialEvent) item2.getItem();
                            if (s1.id.equals(s2.id)) {
                                return Double.MAX_VALUE;
                            }
                            return s1.distance(s2);
                        }
                    });
            threshold = Math.max(threshold, getDistance(source, nearest));
        }

        return threshold * 1.0001;
    }

    public double getMinimumDistance(List<SpatialEvent> srcEvents, SpatialEvent curEvent) {
        double minDistance = Double.MAX_VALUE;
        for (SpatialEvent destEvent : srcEvents) {
            if (destEvent.id.equals(curEvent.id)) {
                continue;
            }

            minDistance = Math.min(minDistance, getDistance(curEvent, destEvent, distanceType));
            if (minDistance == 0d) {
                return minDistance;
            }
        }
        return minDistance;
    }

    public double getMaximumDistance(List<SpatialEvent> srcEvents, SpatialEvent curEvent) {
        double maxDistance = Double.MIN_VALUE;
        for (SpatialEvent destEvent : srcEvents) {
            if (destEvent.id.equals(curEvent.id)) {
                continue;
            }
            maxDistance = Math.max(maxDistance, getDistance(curEvent, destEvent, distanceType));
        }
        return maxDistance;
    }

    public SpatialEvent getMeanCenter(List<SpatialEvent> spatialEventSet, boolean useWeight) {
        double sumX = 0.0, sumY = 0.0, sumWeight = 0.0;
        double weight = 1;

        for (SpatialEvent curEvent : spatialEventSet) {
            weight = useWeight ? curEvent.xVal : 1;
            sumWeight += weight;
            sumX += (curEvent.getCoordinate().x * weight);
            sumY += (curEvent.getCoordinate().y * weight);
        }

        double centerX = sumX / sumWeight;
        double centerY = sumY / sumWeight;

        return new SpatialEvent(0, new Coordinate(centerX, centerY), 0);
    }

    public List<SpatialEvent> getCentralFeature(List<SpatialEvent> spatialEventSet,
            boolean useWeight) {
        List<SpatialEvent> centralEvents = new ArrayList<SpatialEvent>();

        if (spatialEventSet.size() <= 2) {
            return spatialEventSet;
        } else {
            double minDistance = Double.MAX_VALUE;
            SpatialEvent centralEvent = new SpatialEvent(0);

            for (SpatialEvent curEvent : spatialEventSet) {
                double curDistance = 0;

                for (SpatialEvent destEvent : spatialEventSet) {
                    if (curEvent.id != destEvent.id) {
                        if (useWeight) {
                            curDistance += getDistance(curEvent, destEvent, distanceType)
                                    * destEvent.xVal;
                        } else {
                            curDistance += getDistance(curEvent, destEvent, distanceType);
                        }
                    }
                }

                if (minDistance > curDistance) {
                    minDistance = curDistance;
                    centralEvent = curEvent;
                }
            }
            centralEvents.add(centralEvent);
        }

        return centralEvents;
    }

    public SpatialEvent getStandardDistance(List<SpatialEvent> spatialEventSet,
            double standardDeviation, boolean useWeight) {
        double diffX = 0, diffY = 0, diffDist = 0;
        double sumWeight = 0.0;
        double weight = 1.0;

        SpatialEvent centerPoint = getMeanCenter(spatialEventSet, useWeight);

        for (SpatialEvent curEvent : spatialEventSet) {
            weight = useWeight ? curEvent.xVal : 1.0;
            sumWeight += weight;

            diffX = curEvent.getCoordinate().x - centerPoint.getCoordinate().x;
            diffY = curEvent.getCoordinate().y - centerPoint.getCoordinate().y;

            diffDist += (diffX * diffX * weight) + (diffY * diffY * weight);
        }
        double stdDistance = Math.sqrt(diffDist / sumWeight) * standardDeviation;

        return new SpatialEvent(0, centerPoint.getCoordinate(), stdDistance);
    }

    public Geometry getStandardDistanceAsCircle(List<SpatialEvent> spatialEventSet,
            double standardDeviation, boolean useWeight) {
        SpatialEvent sdtPoint = getStandardDistance(spatialEventSet, standardDeviation, useWeight);
        Point centerPoint = gf.createPoint(sdtPoint.getCoordinate());

        // for degree in NUM.arange(0, 360):
        return centerPoint.buffer(sdtPoint.xVal * standardDeviation, 90, 1);
    }

    public double getDistance(SpatialEvent origEvent, SpatialEvent destEvent) {
        switch (distanceType) {
        case Euclidean:
            return getEuclideanDistance(origEvent, destEvent);
        case Manhattan:
            return getManhattanDistance(origEvent, destEvent);
        }
        return getEuclideanDistance(origEvent, destEvent);
    }

    public double getDistance(SpatialEvent origEvent, SpatialEvent destEvent,
            DistanceMethod distanceMethod) {
        switch (distanceMethod) {
        case Euclidean:
            return getEuclideanDistance(origEvent, destEvent);
        case Manhattan:
            return getManhattanDistance(origEvent, destEvent);
        }
        return getEuclideanDistance(origEvent, destEvent);
    }

    public double getEuclideanDistance(SpatialEvent origEvent, SpatialEvent destEvent) {
        return origEvent.distance(destEvent);
    }

    public double getEuclideanDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public double getManhattanDistance(SpatialEvent origEvent, SpatialEvent destEvent) {
        // it is |x1 - x2| + |y1 - y2|.
        double dx = Math.abs(origEvent.getCoordinate().x - destEvent.getCoordinate().x);
        double dy = Math.abs(origEvent.getCoordinate().y - destEvent.getCoordinate().y);
        return dx + dy;
    }

    public static List<SpatialEvent> loadEvents(SimpleFeatureSource featureSource, Filter filter,
            String weightField) {
        filter = filter == null ? Filter.INCLUDE : filter;
        SimpleFeatureCollection features = null;
        try {
            features = featureSource.getFeatures(filter);
            return loadEvents(features, weightField);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return null;
    }

    public static List<SpatialEvent> loadEvents(SimpleFeatureCollection features,
            String weightField) {
        List<SpatialEvent> events = new ArrayList<SpatialEvent>();

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        weightField = FeatureTypes.validateProperty(features.getSchema(), weightField);
        Expression expression = ff.property(weightField);

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry origGeom = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = origGeom.getCentroid().getCoordinate();

                Double weight = expression.evaluate(feature, Double.class);

                SpatialEvent sEvent = new SpatialEvent(feature.getID(), coordinate);
                sEvent.xVal = weight == null ? 1.0 : weight;
                events.add(sEvent);
            }
        } finally {
            featureIter.close();
        }

        return events;
    }
}
