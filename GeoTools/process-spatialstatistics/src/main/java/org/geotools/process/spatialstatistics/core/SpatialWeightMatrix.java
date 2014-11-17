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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
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

    public List<SpatialEvent> Events;

    protected double beta = 1.0;

    protected double[] rowSum;

    public double distanceBandWidth = 0;

    public double dZSum = 0;

    public double dZ2Sum = 0;

    public double dZ3Sum = 0;

    public double dZ4Sum = 0;

    protected SpatialConcept spatialConcept = SpatialConcept.INVERSEDISTANCE;

    protected StandardizationMethod standardizationMethod = StandardizationMethod.NONE;

    protected DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    DistanceFactory factory = DistanceFactory.newInstance();

    public SpatialWeightMatrix() {
    }

    public SpatialWeightMatrix(SpatialConcept spatialConcept,
            StandardizationMethod standardizationType) {
        this.spatialConcept = spatialConcept;
        this.standardizationMethod = standardizationType;

        if (spatialConcept == SpatialConcept.INVERSEDISTANCESQUARED) {
            beta = 2.0;
        } else {
            beta = 1.0;
        }
    }

    public void buildWeightMatrix(SimpleFeatureCollection inputFeatures, String obsField,
            DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;

        Events = loadEvents(inputFeatures, obsField);

        // Find Maximum Nearest Neighbor Distance

        if (distanceBandWidth == 0) {
            factory.DistanceType = distanceMethod;
            double threshold = Double.MIN_VALUE;
            for (SpatialEvent curEvent : Events) {
                double nnDist = factory.getMinimumDistance(Events, curEvent);
                threshold = Math.max(nnDist, threshold);
            }

            // #### Increase For Rounding Error #### 2369.39576291193
            distanceBandWidth = threshold * 1.0001;
            LOGGER.log(Level.WARNING, "The default neighborhood search threshold was "
                    + distanceBandWidth);
        }

        if (standardizationMethod == StandardizationMethod.ROW) {
            this.rowSum = new double[Events.size()];
            for (SpatialEvent curE : Events) {
                this.rowSum[curE.oid] = getRowSum(curE);
            }
        }
    }

    public double getWeight(SpatialEvent origEvent, SpatialEvent destEvent) {
        double dDist = factory.getDistance(origEvent, destEvent, distanceMethod);

        double dWeight = dDist; // default

        // Converts a distance to a weight based on user specified concept of
        // spatial relationships and threshold distance (if any)."""
        // HelperFunctions.py - 272 line
        switch (spatialConcept) {
        case INVERSEDISTANCE:
            dWeight = dDist <= 1.0 ? 1.0 : 1.0 / (Math.pow(dDist, beta));
            break;
        case FIXEDDISTANCEBAND:
            dWeight = dDist <= distanceBandWidth ? 1.0 : 0.0;
            break;
        case ZONEOFINDIFFERENCE:
            dWeight = dDist > distanceBandWidth ? 1.0 / ((dDist - distanceBandWidth) + 1) : 1.0;
            break;
        case INVERSEDISTANCESQUARED:
        case POLYGONCONTIGUITY:
        case SPATIALWEIGHTSFROMFILE:
            // default distance
            break;
        }

        if (!((spatialConcept == SpatialConcept.ZONEOFINDIFFERENCE) || (spatialConcept == SpatialConcept.FIXEDDISTANCEBAND))) {
            if (distanceBandWidth > 0 && dDist > distanceBandWidth) {
                dWeight = 0.0;
            }
        }

        return dWeight;
    }

    public double standardizeWeight(SpatialEvent origEvent, double dWeight) {
        switch (standardizationMethod) {
        case NONE:
            return dWeight;
        case ROW:
            return dWeight / getRowSum(origEvent);
        case GLOBAL:
            return dWeight / this.dZSum;
        }
        return dWeight;
    }

    protected double getRowSum(SpatialEvent origEvent) {
        double returnSum = 0.0;
        for (SpatialEvent curE : Events) {
            if (origEvent.oid != curE.oid) {
                returnSum += getWeight(origEvent, curE);
            }
        }
        return returnSum;
    }

    protected double getValue(SimpleFeature feature, Expression attrExpr) {
        Double valObj = attrExpr.evaluate(feature, Double.class);
        if (valObj != null) {
            return valObj;
        }
        return Double.valueOf(1.0);
    }

    private List<SpatialEvent> loadEvents(SimpleFeatureCollection features, String obsField) {
        List<SpatialEvent> srcEvents = new ArrayList<SpatialEvent>();

        this.dZSum = this.dZ2Sum = this.dZ3Sum = this.dZ4Sum = 0.0;

        obsField = FeatureTypes.validateProperty(features.getSchema(), obsField);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Expression obsExpression = ff.property(obsField);

        int oid = 0;
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                SpatialEvent sEvent = new SpatialEvent(oid++, coordinate);
                sEvent.weight = getValue(feature, obsExpression);

                dZSum += sEvent.weight;
                dZ2Sum += Math.pow(sEvent.weight, 2.0);
                dZ3Sum += Math.pow(sEvent.weight, 3.0);
                dZ4Sum += Math.pow(sEvent.weight, 4.0);
                srcEvents.add(sEvent);
            }
        } finally {
            featureIter.close();
        }

        return srcEvents;
    }
}
