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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.process.spatialstatistics.enumeration.ContiguityType;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * SpatialWeightMatrix
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(SpatialWeightMatrix.class);

    public double dZSum = 0;

    public double dZ2Sum = 0;

    public double dZ3Sum = 0;

    public double dZ4Sum = 0;

    protected List<SpatialEvent> events;

    protected double beta = 1.0; // 1 or 2

    protected Hashtable<Object, Double> rowSum = new Hashtable<Object, Double>();

    protected double distanceBandWidth = 0;

    protected SpatialConcept spatialConcept = SpatialConcept.InverseDistance;

    private boolean isContiguity = false;

    private SpatialWeightMatrixResult swmContiguity;

    protected StandardizationMethod standardizationMethod = StandardizationMethod.None;

    protected boolean selfContains = false;

    protected DistanceFactory factory = DistanceFactory.newInstance();

    public SpatialWeightMatrix() {
    }

    public SpatialWeightMatrix(SpatialConcept spatialConcept,
            StandardizationMethod standardizationMethod) {
        this.spatialConcept = spatialConcept;
        this.standardizationMethod = standardizationMethod;
        this.beta = spatialConcept == SpatialConcept.InverseDistanceSquared ? 2.0 : 1.0;
        this.isContiguity = spatialConcept == SpatialConcept.ContiguityEdgesNodes
                || spatialConcept == SpatialConcept.ContiguityEdgesOnly
                || spatialConcept == SpatialConcept.ContiguityNodesOnly;
    }

    public List<SpatialEvent> getEvents() {
        return this.events;
    }

    public double getDistanceBandWidth() {
        return distanceBandWidth;
    }

    public void setDistanceBandWidth(double distanceBandWidth) {
        this.distanceBandWidth = distanceBandWidth;
    }

    public DistanceMethod getDistanceMethod() {
        return factory.getDistanceType();
    }

    public void setDistanceMethod(DistanceMethod distanceMethod) {
        factory.setDistanceType(distanceMethod);
    }

    public boolean isSelfContains() {
        return selfContains;
    }

    public void setSelfContains(boolean selfContains) {
        this.selfContains = selfContains;
    }

    public void buildWeightMatrix(SimpleFeatureCollection inputFeatures, String obsField) {
        this.events = loadEvents(inputFeatures, obsField);

        if (isContiguity) {
            SpatialWeightMatrixContiguity contiguity = new SpatialWeightMatrixContiguity();
            contiguity.setSelfContains(isSelfContains());
            if (spatialConcept == SpatialConcept.ContiguityEdgesNodes) {
                contiguity.setContiguityType(ContiguityType.Queen);
            } else if (spatialConcept == SpatialConcept.ContiguityEdgesOnly) {
                contiguity.setContiguityType(ContiguityType.Rook);
            } else if (spatialConcept == SpatialConcept.ContiguityNodesOnly) {
                contiguity.setContiguityType(ContiguityType.Bishops);
            }
            swmContiguity = contiguity.execute(inputFeatures, null);
        }

        if (spatialConcept == SpatialConcept.KNearestNeighbors) {
            SpatialWeightMatrixKNearestNeighbors swmKnearest = new SpatialWeightMatrixKNearestNeighbors();
            swmKnearest.setSelfContains(isSelfContains());
            swmContiguity = swmKnearest.execute(inputFeatures, null);
        }

        if (!isContiguity && distanceBandWidth == 0) {
            calculateDistanceBand();
        }

        if (standardizationMethod == StandardizationMethod.Row) {
            calculateRowSum();
        }
    }

    public double getWeight(SpatialEvent origEvent, SpatialEvent destEvent) {
        double weight = 0.0; // default

        if (isContiguity) {
            weight = swmContiguity.isNeighbor(origEvent.oid, destEvent.oid) ? 1.0 : 0.0;
        } else {
            double dist = factory.getDistance(origEvent, destEvent);
            if (spatialConcept == SpatialConcept.InverseDistance) {
                weight = dist <= 1.0 ? 1.0 : 1.0 / (Math.pow(dist, beta)); // beta = 1
            } else if (spatialConcept == SpatialConcept.InverseDistanceSquared) {
                weight = dist <= 1.0 ? 1.0 : 1.0 / (Math.pow(dist, beta)); // beta = 2
            } else if (spatialConcept == SpatialConcept.FixedDistance) {
                weight = dist <= distanceBandWidth ? 1.0 : 0.0;
            } else if (spatialConcept == SpatialConcept.ZoneOfIndifference) {
                weight = dist > distanceBandWidth ? 1.0 / ((dist - distanceBandWidth) + 1) : 1.0;
            } else if (spatialConcept == SpatialConcept.KNearestNeighbors) {
                weight = swmContiguity.isNeighbor(origEvent.oid, destEvent.oid) ? 1.0 : 0.0;
            }
        }

        return weight;
    }

    public double standardizeWeight(SpatialEvent origEvent, double dWeight) {
        if (standardizationMethod == StandardizationMethod.Row) {
            Double rowSum = this.rowSum.get(origEvent.oid);
            return rowSum == 0 ? 0.0 : dWeight / rowSum;
        }
        return dWeight;
    }

    protected void calculateRowSum() {
        this.rowSum.clear();
        for (SpatialEvent origEvent : events) {
            this.rowSum.put(origEvent.oid, getRowSum(origEvent));
        }
    }

    protected double getRowSum(SpatialEvent origEvent) {
        double rowSum = 0.0;
        for (SpatialEvent curEvent : events) {
            if (!selfContains && origEvent.oid == curEvent.oid) {
                continue;
            }
            rowSum += getWeight(origEvent, curEvent);
        }
        return rowSum;
    }

    protected double getValue(SimpleFeature feature, Expression expression) {
        Double value = expression.evaluate(feature, Double.class);
        if (value == null) {
            return Double.valueOf(1.0);
        }
        return value;
    }

    protected void calculateDistanceBand() {
        double threshold = Double.MIN_VALUE;
        for (SpatialEvent curEvent : events) {
            threshold = Math.max(threshold, factory.getMinimumDistance(events, curEvent));
        }
        distanceBandWidth = threshold * 1.0001;
        LOGGER.log(Level.WARNING, "The default neighborhood search threshold was "
                + distanceBandWidth);
    }

    private List<SpatialEvent> loadEvents(SimpleFeatureCollection features, String obsField) {
        List<SpatialEvent> srcEvents = new ArrayList<SpatialEvent>();

        this.dZSum = this.dZ2Sum = this.dZ3Sum = this.dZ4Sum = 0.0;

        obsField = FeatureTypes.validateProperty(features.getSchema(), obsField);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Expression obsExpression = ff.property(obsField);

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                SpatialEvent event = new SpatialEvent(feature.getID(), coordinate);
                event.weight = getValue(feature, obsExpression);

                dZSum += event.weight;
                dZ2Sum += Math.pow(event.weight, 2.0);
                dZ3Sum += Math.pow(event.weight, 3.0);
                dZ4Sum += Math.pow(event.weight, 4.0);
                srcEvents.add(event);
            }
        } finally {
            featureIter.close();
        }

        return srcEvents;
    }
}
