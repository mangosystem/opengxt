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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Geary's C statistic.
 * 
 * @reference http://www.passagesoftware.net/webhelp/Geary_s_c.htm
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalGearysCOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGearysCOperation.class);

    private SpatialWeightMatrix swMatrix = null;

    public GlobalGearysCOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public GearysC execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double dSumWC = 0.0;
        double dSumW = 0.0;
        double dM2 = 0.0;
        double dM4 = 0.0;
        double dSumS1 = 0.0;
        double dSumS2 = 0.0;

        double n = swMatrix.getEvents().size() * 1.0;
        double dZMean = swMatrix.dZSum / n;

        for (SpatialEvent curE : swMatrix.getEvents()) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;
            double dZiDeviation = curE.weight - dZMean;
            dM2 += Math.pow(dZiDeviation, 2.0);
            dM4 += Math.pow(dZiDeviation, 4.0);

            for (SpatialEvent destE : swMatrix.getEvents()) {
                if (curE.oid == destE.oid)
                    continue;

                // For Geary, the cross-product uses the actual values themselves at each location
                // (xi - xj)^2
                double dCij = Math.pow(curE.weight - destE.weight, 2.0);

                // Calculate the weight (dWij)
                double dWij = swMatrix.getWeight(curE, destE);
                double dWji = dWij;

                if (getStandardizationType() == StandardizationMethod.Row) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                    dWji = swMatrix.standardizeWeight(destE, dWji);
                }

                // Create sums needed to calculate
                dSumWC += dWij * dCij;
                dSumW += dWij;
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
                dSumS1 += Math.pow(dWij + dWji, 2.0);
            }
            dSumS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
        }
        dSumS1 = 0.5 * dSumS1;

        dM2 = dM2 / (n - 1.0);
        dM4 = dM4 / (n - 1.0);

        // Geary’s c ranges from zero to infinity, with an expected value of 1 under no autocorrelation.
        // Values from zero to one indicate positive spatial autocorrelation,
        // values above 1 negative spatial autocorrelation.
        double b2 = dM4 / (dM2 * dM2);
        double dExpected = 1.0;

        if (dSumW <= 0.0) {
            GearysC gearysC = new GearysC(0d, dExpected, 0d);
            gearysC.setConceptualization(getSpatialConceptType());
            gearysC.setDistanceMethod(getDistanceType());
            gearysC.setRowStandardization(getStandardizationType());
            gearysC.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return gearysC;
        }

        // Finally, we can calculate
        double dGearysC = dSumWC / (2.0 * dM2 * dSumW);

        // variance of c
        double W2 = Math.pow(dSumW, 2.0);
        double n2 = Math.pow(n, 2.0);
        double div = n * (n - 2.0) * (n - 3.0);
        double A = ((n - 1) * dSumS1 * (n2 - (3.0 * n) + 3.0 - ((n - 1) * b2))) / (div * W2);
        double B = ((n - 1) * dSumS2 * (n2 + (3.0 * n) - 6.0 - ((n2 - n + 2) * b2)))
                / (4.0 * div * W2);
        double C = (n2 - 3.0 - (Math.pow(n - 1, 2.0) * b2)) / div;

        double rVariance = A - B + C;

        // finally build result
        GearysC gearysC = new GearysC(dGearysC, dExpected, rVariance);
        gearysC.setConceptualization(getSpatialConceptType());
        gearysC.setDistanceMethod(getDistanceType());
        gearysC.setRowStandardization(getStandardizationType());
        gearysC.setDistanceThreshold(swMatrix.getDistanceBandWidth());

        return gearysC;
    }

    public static final class GearysC {

        double observedIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

        double distanceThreshold = 0.0;

        public GearysC() {
        }

        public GearysC(double observedIndex, double expectedIndex, double zVariance) {
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
