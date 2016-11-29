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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Moran's I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalMoranIStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalMoranIStatisticOperation.class);

    private SpatialWeightMatrix swMatrix = null;

    public GlobalMoranIStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public MoransI execute(SimpleFeatureCollection inputFeatures, String inputField) {
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

        // Calculate sample mean.
        double n = swMatrix.getEvents().size() * 1.0;
        double dZMean = swMatrix.dZSum / n;

        for (SpatialEvent curE : swMatrix.getEvents()) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;
            double dZiDeviation = curE.weight - dZMean;
            dM2 += Math.pow(dZiDeviation, 2.0);
            dM4 += Math.pow(dZiDeviation, 4.0);

            for (SpatialEvent destE : swMatrix.getEvents()) {
                if (curE.oid == destE.oid) {
                    continue;
                }

                double dZjDeviation = destE.weight - dZMean;
                double dCij = dZiDeviation * dZjDeviation;

                // Calculate the weight (dWij)
                double dWij = swMatrix.getWeight(curE, destE);
                double dWji = dWij;

                if (getStandardizationType() == StandardizationMethod.Row) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                    dWji = swMatrix.standardizeWeight(destE, dWji);
                }

                // Create sums needed to calculate Moran's I
                dSumWC += dWij * dCij;
                dSumW += dWij;
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
                dSumS1 += Math.pow(dWij + dWji, 2.0);
            }

            dSumS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
        }
        dSumS1 = 0.5 * dSumS1;

        // we need a few more working variables:
        dM2 = (dM2 * 1.0) / n; // # standard deviation
        dM4 = (dM4 * 1.0) / n;

        double dB2 = dM4 / (dM2 * dM2); // sample kurtosis
        double dExpected = -1.0 / (n - 1.0); // Expected Moran's I

        if (dSumW <= 0.0) {
            MoransI moransI = new MoransI(0d, dExpected, 0d);
            moransI.setConceptualization(getSpatialConceptType());
            moransI.setDistanceMethod(getDistanceType());
            moransI.setRowStandardization(getStandardizationType());
            moransI.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return moransI;
        }

        // Finally, we can calculate Moran's I and its significance (Z Score).
        // This Z Score is based on the calculated RANDOMIZATION null hypothesis.
        double dMoranI = dSumWC / (dM2 * dSumW);

        double dDiv = ((n - 1.0) * (n - 2.0) * (n - 3.0) * (Math.pow(dSumW, 2.0)));
        double dTmp1 = n
                * ((Math.pow(n, 2.0) - (3.0 * n) + 3.0) * dSumS1 - (n * dSumS2) + 3.0 * (Math.pow(
                        dSumW, 2.0)));
        double dTmp2 = dB2
                * ((Math.pow(n, 2.0) - n) * dSumS1 - (2.0 * n * dSumS2) + 6.0 * (Math.pow(dSumW,
                        2.0)));

        double rVariance = (dTmp1 / dDiv) - (dTmp2 / dDiv) - (Math.pow(dExpected, 2.0));

        // finally build result
        MoransI moransI = new MoransI(dMoranI, dExpected, rVariance);
        moransI.setConceptualization(getSpatialConceptType());
        moransI.setDistanceMethod(getDistanceType());
        moransI.setRowStandardization(getStandardizationType());
        moransI.setDistanceThreshold(swMatrix.getDistanceBandWidth());

        return moransI;
    }

    public static final class MoransI {

        double observedIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

        double distanceThreshold = 0.0;

        public MoransI() {
        }

        public MoransI(double observedIndex, double expectedIndex, double zVariance) {
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
