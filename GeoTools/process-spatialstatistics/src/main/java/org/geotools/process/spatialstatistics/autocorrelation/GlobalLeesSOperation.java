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
import org.geotools.process.spatialstatistics.core.SpatialWeightMatrix;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;

/**
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Lee's S statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalLeesSOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalLeesSOperation.class);

    private SpatialWeightMatrix swMatrix = null;

    public GlobalLeesSOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public LeesS execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double dSumWC = 0.0; // sum of weighted co-variance (dWij * dCij)
        double dSumW = 0.0; // sum of all weights (dWij)
        double dSumWC2 = 0.0;
        double dSumW2 = 0.0;
        double dM2 = 0.0;
        double dM4 = 0.0;
        double dSumS1 = 0.0;
        double dSumS2 = 0.0;

        double n = swMatrix.getEvents().size();
        double dZMean = swMatrix.dZSum / n;

        for (SpatialEvent curE : swMatrix.getEvents()) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;
            double dWCijSum = 0.0;
            double dZiDeviation = curE.weight - dZMean;
            dM2 += Math.pow(dZiDeviation, 2.0);
            dM4 += Math.pow(dZiDeviation, 4.0);

            for (SpatialEvent destE : swMatrix.getEvents()) {
                if (curE.oid == destE.oid)
                    continue;

                double dCij = destE.weight - dZMean;

                // Calculate the weight (dWij)
                double dWij = swMatrix.getWeight(curE, destE);
                double dWji = dWij;

                if (getStandardizationType() == StandardizationMethod.Row) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                    dWji = swMatrix.standardizeWeight(destE, dWji);
                }

                // Create sums needed to calculate
                dSumWC += dWij * dCij;
                dWCijSum += dWij * dCij;
                dSumW += dWij;
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
                dSumS1 += Math.pow(dWij + dWji, 2.0);
            }
            dSumS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
            dSumW2 += Math.pow(dWijS2Sum, 2.0);
            dSumWC2 += Math.pow(dWCijSum, 2.0);
        }
        dSumS1 = 0.5 * dSumS1;

        dM2 = dM2 / n;
        dM4 = dM4 / n;

        // TODO modify
        // Expected values = 0.1711
        // E.S.off = (f0.off * g0.off) / (n * (n - 1))
        // E.S.on = (f0.on * g0.on) / n
        // E.S = E.S.off + E.S.on
        double f0off = dSumW;
        double f0on = 0.0;
        double g0off = dSumWC;
        double g0on = 0.0;
        double eoff = (f0off * g0off) / (n * (n - 1));
        double eon = (f0on * g0on) / n;
        double dExpected = (eoff) + (eon);

        if (dSumW <= 0.0) {
            LeesS leesS = new LeesS(0d, dExpected, 0d);
            leesS.setConceptualization(getSpatialConceptType());
            leesS.setDistanceMethod(getDistanceType());
            leesS.setRowStandardization(getStandardizationType());
            leesS.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return leesS;
        }

        // Finally, we can calculate
        double dLeesS = dSumWC2 / (dM2 * dSumW2);

        // TODO modify
        // variance
        double f1off = dSumS1;
        double f2off = 0;
        double g1off = 0;
        double g2off = 0;
        double rVariance = ((2 * f1off * g1off) / (n * (n - 1)))
                + ((4 * (f2off - f1off) * (g2off - g1off)) / (n * (n - 1) * (n - 2)))
                + ((((f0off * f0off) + (2 * f1off) - (4 * f2off)) * ((g0off * g0off) + (2 * g1off) - (4 * g2off))) / (n
                        * (n - 1) * (n - 2) * (n - 3))) - (eoff * eoff);

        rVariance = 0.0; // temp

        // finally build result
        LeesS leesS = new LeesS(dLeesS, dExpected, rVariance);
        leesS.setConceptualization(getSpatialConceptType());
        leesS.setDistanceMethod(getDistanceType());
        leesS.setRowStandardization(getStandardizationType());
        leesS.setDistanceThreshold(swMatrix.getDistanceBandWidth());

        return leesS;
    }

    public static final class LeesS {

        double observedIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

        double distanceThreshold = 0.0;

        public LeesS() {
        }

        public LeesS(double observedIndex, double expectedIndex, double zVariance) {
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
