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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Lee's L statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalLeesLOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalLeesLOperation.class);

    public GlobalLeesLOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(true);
    }

    public LeesL execute(SimpleFeatureCollection inputFeatures, String xField, String yField) {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, xField, yField);

        double wijSum = 0.0; // sum of all weights (wij)
        double wijSum2 = 0.0;
        double jxydWSum = 0.0;
        double zxiSum2 = 0.0;
        double zyiSum2 = 0.0;

        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;
        double meanY = swMatrix.sumY / n;

        for (SpatialEvent source : swMatrix.getEvents()) {
            double jwijSum = 0.0;
            double zxjWSum = 0.0;
            double zyjWSum = 0.0;

            double zxi = source.xVal - meanX;
            double zyi = source.yVal - meanY;
            zxiSum2 += Math.pow(zxi, 2.0);
            zyiSum2 += Math.pow(zyi, 2.0);

            for (SpatialEvent target : swMatrix.getEvents()) {
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }

                double wij = swMatrix.getWeight(source, target);
                wij = swMatrix.standardizeWeight(source, wij);
                if (wij == 0) {
                    continue;
                }

                double zxj = target.xVal - meanX;
                double zyj = target.yVal - meanY;

                zxjWSum += wij * zxj;
                zyjWSum += wij * zyj;
                jwijSum += wij;
            }

            wijSum += jwijSum;
            wijSum2 += Math.pow(jwijSum, 2.0);
            jxydWSum += Math.abs(zxjWSum * zyjWSum);
        }

        // TODO modify
        double dExpected = 0.0;

        if (wijSum == 0.0) {
            LeesL leesL = new LeesL(0d, dExpected, 0d);
            leesL.setConceptualization(getSpatialConceptType());
            leesL.setDistanceMethod(getDistanceType());
            leesL.setRowStandardization(getStandardizationType());
            leesL.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return leesL;
        }

        double dObserved = (n / wijSum2) * (jxydWSum / (Math.sqrt(zxiSum2) * Math.sqrt(zyiSum2)));

        // TODO modify
        double zVariance = 0.0;

        // finally build result
        LeesL leesL = new LeesL(dObserved, dExpected, zVariance);
        leesL.setConceptualization(getSpatialConceptType());
        leesL.setDistanceMethod(getDistanceType());
        leesL.setRowStandardization(getStandardizationType());
        leesL.setDistanceThreshold(swMatrix.getDistanceBandWidth());

        return leesL;
    }

    public static final class LeesL {

        double observedIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

        double distanceThreshold = 0.0;

        public LeesL() {
        }

        public LeesL(double observedIndex, double expectedIndex, double zVariance) {
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
    }

}
