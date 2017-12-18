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
package org.geotools.process.spatialstatistics.autocorrelation;

import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.SSUtils.StatEnum;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;

/**
 * Detect spatial clusters based on feature locations and attribute values using the Global Rogerson's R statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalRogersonROperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalRogersonROperation.class);

    private double kappa = 1.0; // default

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public GlobalRogersonROperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(true);
        this.setKappa(1.0);
    }

    public RogersonR execute(SimpleFeatureCollection inputFeatures, String caseField,
            String popField) {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, caseField, popField);

        // spatial version of the chi-square goodness-of-fit statistic
        int featureCount = swMatrix.getEvents().size();
        double m = swMatrix.getEvents().size();

        // R = Goodness-Of-Fit (GOF) and Spatial Association (SA)
        // Rogerson (1999) partitioned Tango’s index, T, into the sum of a goodness-of-fit(GOF) and a Spatial Association(SA) component

        double gof = 0.0;
        double sa = 0.0;
        double aijSum = 0.0;
        double yijyjiSum = 0.0;

        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            double ri = source.xVal / swMatrix.sumX;
            double pi = source.yVal / swMatrix.sumY;
            double ripi = ri - pi;

            gof += (ripi * ripi) / pi;

            double ajSum = 0.0;
            double yijSum = 0.0;
            double yjiSum = 0.0;

            for (int j = 0; j < featureCount; j++) {
                SpatialEvent target = swMatrix.getEvents().get(j);
                if (source.id == target.id) {
                    continue; // gof
                }

                double rj = target.xVal / swMatrix.sumX;
                double pj = target.yVal / swMatrix.sumY;
                double rjpj = rj - pj;
                double pipjSqrt = Math.sqrt(pi * pj);

                double dij = factory.getDistance(source, target);
                double aij = dij <= 1.0 ? 1.0 : 1.0 / Math.pow(dij, kappa);
                double wij = aij / pipjSqrt;

                sa += wij * (ripi * rjpj);

                if (j < i) {
                    ajSum += aij * pipjSqrt;
                }

                // TODO Verify
                yijSum += ((1.0 - pj) * aij * Math.sqrt(pj / pi)) - (pj * sumKJ(target, pi));

                yjiSum += ((1.0 - pi) * aij * Math.sqrt(pi / pj)) - (pi * sumKJ(source, pj));
            }

            if (i > 0) {
                aijSum += ajSum;
            }

            yijyjiSum += yijSum * yjiSum;
        }

        double dObserved = gof + sa;

        double dExpected = (m - 1.0 - (2.0 * aijSum)) / swMatrix.sumX;

        double zVariance = (2.0 / Math.pow(swMatrix.sumX, 2.0)) * yijyjiSum;

        // finally build result
        RogersonR rogersonR = new RogersonR(dObserved, dExpected, zVariance);
        rogersonR.setConceptualization(getSpatialConceptType());
        rogersonR.setDistanceMethod(getDistanceType());
        rogersonR.setRowStandardization(getStandardizationType());
        rogersonR.setDistanceThreshold(swMatrix.getDistanceBandWidth());
        rogersonR.setKappa(kappa);

        return rogersonR;
    }

    private double sumKJ(SpatialEvent source, double pi) {
        double sum = 0.0;
        int featureCount = swMatrix.getEvents().size();
        for (int k = 0; k < featureCount; k++) {
            SpatialEvent target = swMatrix.getEvents().get(k);
            if (source.id == target.id) {
                continue;
            }

            double pk = target.yVal / swMatrix.sumY;
            double dik = factory.getDistance(source, target);
            double aik = dik <= 1.0 ? 1.0 : 1.0 / Math.pow(dik, kappa);
            sum += aik * Math.sqrt(pk / pi);
        }
        return sum;
    }

    public static final class RogersonR {

        double observedIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

        double distanceThreshold = 0.0;

        double kappa = 1.0;

        public RogersonR() {
        }

        public RogersonR(double observedIndex, double expectedIndex, double zVariance) {
            setObservedIndex(observedIndex);
            setExpectedIndex(expectedIndex);
            setZVariance(zVariance);
        }

        public void setObservedIndex(double observedIndex) {
            this.observedIndex = observedIndex;
        }

        public double getObservedIndex() {
            return observedIndex;
        }

        public void setExpectedIndex(double expectedIndex) {
            this.expectedIndex = expectedIndex;
        }

        public double getExpectedIndex() {
            return expectedIndex;
        }

        public void setZVariance(double zVariance) {
            this.zVariance = zVariance;
        }

        public double getZVariance() {
            return zVariance;
        }

        public double getZScore() {
            double dX = observedIndex - expectedIndex;
            double dY = Math.pow(getZVariance(), 0.5);
            if (dY == 0)
                return 0.0;

            return dX / dY;
        }

        public double getPValue() {
            return SSUtils.zProb(this.getZScore(), StatEnum.BOTH);
        }

        public SpatialConcept getConceptualization() {
            return conceptualization;
        }

        public void setConceptualization(SpatialConcept conceptualization) {
            this.conceptualization = conceptualization;
        }

        public DistanceMethod getDistanceMethod() {
            return distanceMethod;
        }

        public void setDistanceMethod(DistanceMethod distanceMethod) {
            this.distanceMethod = distanceMethod;
        }

        public StandardizationMethod getRowStandardization() {
            return rowStandardization;
        }

        public void setRowStandardization(StandardizationMethod rowStandardization) {
            this.rowStandardization = rowStandardization;
        }

        public double getDistanceThreshold() {
            return distanceThreshold;
        }

        public void setDistanceThreshold(double distanceThreshold) {
            this.distanceThreshold = distanceThreshold;
        }

        public double getKappa() {
            return kappa;
        }

        public void setKappa(double kappa) {
            this.kappa = kappa;
        }
    }

}
