package org.geotools.process.spatialstatistics.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Help class for distance calculation
 * 
 * @author Minpa Lee
 * @since 1.0
 * @version $Id: DistanceHelper.java 1 2011-09-01 11:22:29Z minpa.lee $
 */
public class DistanceFactory {
    protected static final Logger LOGGER = Logging.getLogger(DistanceFactory.class);

    static GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public DistanceMethod DistanceType = DistanceMethod.Euclidean;

    public static DistanceFactory newInstance() {
        return new DistanceFactory();
    }

    public double GetMeanDistance(List<SpatialEvent> spatialEventSet, SpatialEvent curEvent) {
        double sumDistance = 0.0;

        for (SpatialEvent destEvent : spatialEventSet) {
            if (destEvent.oid != curEvent.oid) {
                sumDistance += getDistance(curEvent, destEvent, DistanceType);
            }
        }
        return sumDistance / spatialEventSet.size();
    }

    public double getMinimumDistance(List<SpatialEvent> srcEvents, SpatialEvent curEvent) {
        double minDistance = Double.MAX_VALUE;
        for (SpatialEvent destEvent : srcEvents) {
            if (destEvent.oid != curEvent.oid) {
                minDistance = Math.min(minDistance, getDistance(curEvent, destEvent, DistanceType));
                if (minDistance == 0d) {
                    return minDistance;
                }
            }
        }
        return minDistance;
    }

    public double getMaximumDistance(List<SpatialEvent> srcEvents, SpatialEvent curEvent) {
        double maxDistance = Double.MIN_VALUE;
        for (SpatialEvent destEvent : srcEvents) {
            if (destEvent.oid != curEvent.oid) {
                maxDistance = Math.max(maxDistance, getDistance(curEvent, destEvent, DistanceType));
            }
        }
        return maxDistance;
    }

    public SpatialEvent getMeanCenter(List<SpatialEvent> spatialEventSet, boolean useWeight) {
        double sumX = 0.0, sumY = 0.0, sumWeight = 0.0;
        double weight = 1;

        for (SpatialEvent curEvent : spatialEventSet) {
            weight = useWeight ? curEvent.weight : 1;
            sumWeight += weight;
            sumX += (curEvent.x * weight);
            sumY += (curEvent.y * weight);
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
                    if (curEvent.oid != destEvent.oid) {
                        if (useWeight) {
                            curDistance += getDistance(curEvent, destEvent, DistanceType)
                                    * destEvent.weight;
                        } else {
                            curDistance += getDistance(curEvent, destEvent, DistanceType);
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
        double weight = 1;

        SpatialEvent centerPoint = getMeanCenter(spatialEventSet, useWeight);

        for (SpatialEvent curEvent : spatialEventSet) {
            weight = useWeight ? curEvent.weight : 1;
            sumWeight += weight;

            diffX = curEvent.x - centerPoint.x;
            diffY = curEvent.y - centerPoint.y;
            
            diffDist += (diffX * diffX * weight) + (diffY * diffY * weight);
        }
        double stdDistance = Math.sqrt(diffDist / sumWeight) * standardDeviation;

        return new SpatialEvent(0, new Coordinate(centerPoint.x, centerPoint.y), stdDistance);
    }

    public Geometry getStandardDistanceAsCircle(List<SpatialEvent> spatialEventSet,
            double standardDeviation, boolean useWeight) {
        SpatialEvent sdtPoint = getStandardDistance(spatialEventSet, standardDeviation, useWeight);

        Coordinate coord = new Coordinate(sdtPoint.x, sdtPoint.y);
        Point centerPoint = gf.createPoint(coord);

        // for degree in NUM.arange(0, 360):
        return centerPoint.buffer(sdtPoint.weight * standardDeviation, 90, 1);
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
        return getEuclideanDistance(origEvent.x, origEvent.y, destEvent.x, destEvent.y);
    }

    public double getEuclideanDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public double getManhattanDistance(SpatialEvent origEvent, SpatialEvent destEvent) {
        // it is |x1 - x2| + |y1 - y2|.
        double dx = Math.abs(origEvent.x - destEvent.x);
        double dy = Math.abs(origEvent.y - destEvent.y);
        return dx + dy;
    }

    public static List<SpatialEvent> loadEvents(SimpleFeatureSource featureSource, Filter filter,
            String weightField) {
        filter = filter == null ? Filter.INCLUDE : filter;
        SimpleFeatureCollection features = null;
        try {
            features = featureSource.getFeatures(filter);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return loadEvents(features, weightField);
    }

    public static List<SpatialEvent> loadEvents(SimpleFeatureCollection features, String weightField) {
        List<SpatialEvent> events = new ArrayList<SpatialEvent>();
        int idxField = -1;
        if (!StringHelper.isNullOrEmpty(weightField)) {
            String propertyName = FeatureTypes.validateProperty(features.getSchema(), weightField);
            idxField = features.getSchema().indexOf(propertyName);
        }

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry origGeom = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = origGeom.getCentroid().getCoordinate();

                SpatialEvent sEvent = new SpatialEvent(FeatureTypes.getFID(feature), coordinate);
                if (idxField != -1) {
                    try {
                        sEvent.weight = Double.valueOf(feature.getAttribute(idxField).toString());
                    } catch (NumberFormatException e) {
                        sEvent.weight = 1;
                    }
                }
                events.add(sEvent);
            }
        } finally {
            featureIter.close();
        }

        return events;
    }

}
