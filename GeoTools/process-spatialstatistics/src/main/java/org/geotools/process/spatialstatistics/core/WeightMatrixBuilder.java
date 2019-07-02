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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * SpatialWeightMatrix
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WeightMatrixBuilder {
    protected static final Logger LOGGER = Logging.getLogger(WeightMatrixBuilder.class);

    public double sumX = 0;

    public double sumX2 = 0;

    public double sumX3 = 0;

    public double sumX4 = 0;

    public double sumY = 0;

    public double sumY2 = 0;

    public double sumY3 = 0;

    public double sumY4 = 0;

    private List<SpatialEvent> events;

    private double exponent = 1.0; // 1 or 2

    private Hashtable<Object, Double> rowSum = new Hashtable<Object, Double>();

    private double distanceBandWidth = 0;

    private SpatialConcept spatialConcept = SpatialConcept.InverseDistance;

    private boolean isContiguity = false;

    private WeightMatrix weightMatrix;

    private StandardizationMethod standardizationMethod = StandardizationMethod.None;

    private boolean selfNeighbors = false;

    private DistanceFactory factory = DistanceFactory.newInstance();

    public WeightMatrixBuilder() {

    }

    public WeightMatrixBuilder(SpatialConcept spatialConcept,
            StandardizationMethod standardizationMethod) {
        this.spatialConcept = spatialConcept;
        this.standardizationMethod = standardizationMethod;
        this.exponent = spatialConcept == SpatialConcept.InverseDistanceSquared ? 2.0 : 1.0;
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

    public boolean isSelfNeighbors() {
        return selfNeighbors;
    }

    public void setSelfNeighbors(boolean selfNeighbors) {
        this.selfNeighbors = selfNeighbors;
    }

    public WeightMatrix getWeightMatrix() {
        return weightMatrix;
    }

    public double getMeanX() {
        return this.sumX / this.getEvents().size();
    }

    public double getMeanY() {
        return this.sumY / this.getEvents().size();
    }

    public WeightMatrix buildWeightMatrix(SimpleFeatureCollection inputFeatures, String xField) {
        return buildWeightMatrix(inputFeatures, xField, null);
    }

    public WeightMatrix buildWeightMatrix(SimpleFeatureCollection inputFeatures, Expression xField) {
        return buildWeightMatrix(inputFeatures, xField, null);
    }

    public WeightMatrix buildWeightMatrix(SimpleFeatureCollection inputFeatures, String xField,
            String yField) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        Expression xExpression = null;
        if (xField != null && !xField.isEmpty()) {
            xExpression = ff.property(xField);
        }

        Expression yExpression = null;
        if (yField != null && !yField.isEmpty()) {
            yExpression = ff.property(yField);
        }

        return buildWeightMatrix(inputFeatures, xExpression, yExpression);
    }

    public WeightMatrix buildWeightMatrix(SimpleFeatureCollection inputFeatures, Expression xField,
            Expression yField) {
        this.events = loadEvents(inputFeatures, xField, yField);

        if (isContiguity) {
            WeightMatrixContiguity contiguity = new WeightMatrixContiguity();
            contiguity.setSelfNeighbors(isSelfNeighbors());
            if (spatialConcept == SpatialConcept.ContiguityEdgesNodes) {
                contiguity.setContiguityType(ContiguityType.Queen);
            } else if (spatialConcept == SpatialConcept.ContiguityEdgesOnly) {
                contiguity.setContiguityType(ContiguityType.Rook);
            } else if (spatialConcept == SpatialConcept.ContiguityNodesOnly) {
                contiguity.setContiguityType(ContiguityType.Bishops);
            }
            weightMatrix = contiguity.execute(inputFeatures, null);
        } else {
            if (spatialConcept == SpatialConcept.KNearestNeighbors) {
                WeightMatrixKNearestNeighbors swmKnearest = new WeightMatrixKNearestNeighbors();
                swmKnearest.setSelfNeighbors(isSelfNeighbors());
                weightMatrix = swmKnearest.execute(inputFeatures, null);
            } else {
                if (distanceBandWidth == 0) {
                    distanceBandWidth = factory.getThresholDistance(inputFeatures);
                }

                WeightMatrixDistance wmsDist = new WeightMatrixDistance();
                wmsDist.setDistanceMethod(getDistanceMethod());
                wmsDist.setSpatialConcept(spatialConcept);
                wmsDist.setStandardizationMethod(standardizationMethod);
                wmsDist.setSelfNeighbors(isSelfNeighbors());
                wmsDist.setThresholdDistance(distanceBandWidth);
                weightMatrix = wmsDist.execute(inputFeatures, null);
            }
        }

        if (standardizationMethod == StandardizationMethod.Row) {
            calculateRowSum();
        }

        return weightMatrix;
    }

    public double getWeight(SpatialEvent source, SpatialEvent target) {
        double weight = 0.0; // default

        if (isContiguity) {
            weight = weightMatrix.isNeighbor(source.id, target.id) ? 1.0 : 0.0;
        } else {
            double dist = factory.getDistance(source, target);
            if (spatialConcept == SpatialConcept.InverseDistance) {
                weight = dist <= 1.0 ? 1.0 : 1.0 / (Math.pow(dist, exponent)); // beta = 1
            } else if (spatialConcept == SpatialConcept.InverseDistanceSquared) {
                weight = dist <= 1.0 ? 1.0 : 1.0 / (Math.pow(dist, exponent)); // beta = 2
            } else if (spatialConcept == SpatialConcept.FixedDistance) {
                weight = dist <= distanceBandWidth ? 1.0 : 0.0;
            } else if (spatialConcept == SpatialConcept.ZoneOfIndifference) {
                weight = dist > distanceBandWidth ? 1.0 / ((dist - distanceBandWidth) + 1) : 1.0;
            } else if (spatialConcept == SpatialConcept.KNearestNeighbors) {
                weight = weightMatrix.isNeighbor(source.id, target.id) ? 1.0 : 0.0;
            }
        }

        return weight;
    }

    public double standardizeWeight(SpatialEvent source, double weight) {
        if (standardizationMethod == StandardizationMethod.Row) {
            Double rowSum = this.rowSum.get(source.id);
            return rowSum == 0 ? 0.0 : weight / rowSum;
        }
        return weight;
    }

    private void calculateRowSum() {
        this.rowSum.clear();
        for (SpatialEvent current : events) {
            this.rowSum.put(current.id, getRowSum(current));
        }
    }

    private double getRowSum(SpatialEvent source) {
        double rowSum = 0.0;
        for (SpatialEvent current : events) {
            if (!selfNeighbors && source.id == current.id) {
                continue;
            }
            rowSum += getWeight(source, current);
        }
        return rowSum;
    }

    private double getValue(SimpleFeature feature, Expression expression) {
        Double value = expression.evaluate(feature, Double.class);
        if (value == null) {
            return Double.valueOf(1.0);
        }
        return value;
    }

    private List<SpatialEvent> loadEvents(SimpleFeatureCollection features, Expression xField,
            Expression yField) {
        List<SpatialEvent> eventList = new ArrayList<SpatialEvent>();

        this.sumX = this.sumX2 = this.sumX3 = this.sumX4 = 0.0;
        this.sumY = this.sumY2 = this.sumY3 = this.sumY4 = 0.0;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                SpatialEvent event = new SpatialEvent(feature.getID(), coordinate);
                event.xVal = getValue(feature, xField);

                sumX += event.xVal;
                sumX2 += Math.pow(event.xVal, 2.0);
                sumX3 += Math.pow(event.xVal, 3.0);
                sumX4 += Math.pow(event.xVal, 4.0);

                if (yField != null) {
                    event.yVal = getValue(feature, yField);

                    sumY += event.yVal;
                    sumY2 += Math.pow(event.yVal, 2.0);
                    sumY3 += Math.pow(event.yVal, 3.0);
                    sumY4 += Math.pow(event.yVal, 4.0);
                }

                eventList.add(event);
            }
        } finally {
            featureIter.close();
        }

        return eventList;
    }
}
