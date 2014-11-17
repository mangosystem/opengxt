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

    SpatialWeightMatrix swMatrix = null;

    public GlobalMoranIStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.INVERSEDISTANCE);
        this.setStandardizationType(StandardizationMethod.NONE);
    }

    public MoransI execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.distanceBandWidth = this.getDistanceBand();
        swMatrix.buildWeightMatrix(inputFeatures, inputField, this.getDistanceType());

        int featureCount = swMatrix.Events.size();
        if (featureCount < 3) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
            return null;
        } else if (featureCount < 30) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
        }

        // """Calculate Moran's Index and Z Score."""
        double dSumWC = 0.0; // # summation of weighted co-variance (dWij * dCij)
        double dSumW = 0.0; // # summation of all weights (dWij)
        double dM2 = 0.0;
        double dM4 = 0.0;
        double dSumS1 = 0.0;
        double dSumS2 = 0.0;
        int lNumObs = swMatrix.Events.size();

        // # Calculate sample mean.
        double rN = lNumObs * 1.0;
        double dZMean = swMatrix.dZSum / rN;
        double dZVar = Math.pow((swMatrix.dZ2Sum / rN) - Math.pow(dZMean, 2), 0.5);
        if (Math.abs(dZVar) <= 0.0) {
            LOGGER.warning("ERROR Zero variance:  all of the values for your input field are likely the same.");
        }

        for (SpatialEvent curE : swMatrix.Events) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;
            double dZiDeviation = curE.weight - dZMean; // # Calculate deviation from mean
            dM2 += Math.pow(dZiDeviation, 2.0);
            dM4 += Math.pow(dZiDeviation, 4.0);

            // # Look for i's local neighbors
            for (SpatialEvent destE : swMatrix.Events) {
                if (curE.oid == destE.oid)
                    continue;

                double dZjDeviation = destE.weight - dZMean;
                double dCij = dZiDeviation * dZjDeviation; // # Calculate ij co-variance

                // # Calculate the weight (dWij)
                double dWij = 0.0;
                double dWji = 0.0;

                if (this.getSpatialConceptType() == SpatialConcept.POLYGONCONTIGUITY) {
                    dWij = 0.0;
                    // if (destE is neighbor ) dWij = 1.0;
                    dWji = dWij;
                } else {
                    dWij = swMatrix.getWeight(curE, destE);
                    dWji = dWij;
                }

                if (getStandardizationType() == StandardizationMethod.ROW) {
                    // dWij = standardize_weight (dWij, inputs, dcRowSum, iKey, dcID, dDistAllSum)
                    // dWji = standardize_weight (dWji, inputs, dcRowSum, jKey, dcID, dDistAllSum)
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                    dWji = swMatrix.standardizeWeight(destE, dWji);
                }

                // # Create sums needed to calculate Moran's I
                dSumWC += dWij * dCij;
                dSumW += dWij;
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
                dSumS1 += Math.pow(dWij + dWji, 2.0);
            }

            dSumS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
        }
        dSumS1 = 0.5 * dSumS1;

        // # we need a few more working variables:
        dM2 = (dM2 * 1.0) / rN; // # standard deviation
        dM4 = (dM4 * 1.0) / rN;

        double dB2 = dM4 / (dM2 * dM2); // # sample kurtosis
        double dExpected = -1.0 / (rN - 1.0); // # Expected Moran's I

        // #### Report if All Features Have No Neighbors ####
        if (dSumW <= 0.0)
            return new MoransI();

        // #### Report on Features with No Neighbors ####

        // # Finally, we can calculate Moran's I and its significance (Z Score).
        // # This Z Score is based on the calculated RANDOMIZATION null hypothesis.

        double dMoranI = dSumWC / (dM2 * dSumW);

        double dDiv = ((rN - 1.0) * (rN - 2.0) * (rN - 3.0) * (Math.pow(dSumW, 2.0)));
        double dTmp1 = rN
                * ((Math.pow(rN, 2.0) - (3.0 * rN) + 3.0) * dSumS1 - (rN * dSumS2) + 3.0 * (Math
                        .pow(dSumW, 2.0)));
        double dTmp2 = dB2
                * ((Math.pow(rN, 2.0) - rN) * dSumS1 - (2.0 * rN * dSumS2) + 6.0 * (Math.pow(dSumW,
                        2.0)));

        double rVariance = (dTmp1 / dDiv) - (dTmp2 / dDiv) - (Math.pow(dExpected, 2.0));

        // finally build result
        MoransI moransI = new MoransI(dMoranI, dExpected, rVariance);
        moransI.setConceptualization(getSpatialConceptType());
        moransI.setDistanceMethod(getDistanceType());
        moransI.setRowStandardization(getStandardizationType());
        moransI.setDistanceThreshold(swMatrix.distanceBandWidth);

        return moransI;
    }

    public final class MoransI {
        // Global Moran's I Summary
        // Moran's Index: 0.260865
        // Expected Index: -0.002288
        // Variance: 0.000353
        // Z Score: 14.008921
        // p-value: 0.000000

        double moransIndex = 0.0;

        double expectedIndex = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.INVERSEDISTANCE;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.NONE;

        double distanceThreshold = 0.0;

        public MoransI() {
        }

        public MoransI(double moransIndex, double expectedIndex, double zVariance) {
            setObservedIndex(moransIndex);
            setExpectedIndex(expectedIndex);
            setZVariance(zVariance);
        }

        public void setObservedIndex(double moransIndex) {
            this.moransIndex = moransIndex;
        }

        public double getObservedIndex() {
            return moransIndex;
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
            // dMoranZScore = (dMoranI - dExpected) / (rVariance**0.5)
            double dX = moransIndex - expectedIndex;
            double dY = Math.pow(getZVariance(), 0.5);
            if (dY == 0)
                return 0.0;

            return dX / dY;
        }

        public double getPValue() {
            // dPVal = STATS.zProb(dZScore, type = 2)
            double zScore = this.getZScore();

            return SSUtils.zProb(zScore, StatEnum.BOTH);
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
