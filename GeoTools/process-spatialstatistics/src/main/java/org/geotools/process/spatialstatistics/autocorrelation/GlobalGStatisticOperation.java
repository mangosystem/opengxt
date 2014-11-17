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

import java.util.logging.Level;
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
 * Measures the degree of clustering for either high values or low values using the Getis-Ord General G statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalGStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGStatisticOperation.class);

    SpatialWeightMatrix swMatrix = null;

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField,
            double distanceBand) {
        this.setDistanceBand(distanceBand);
        this.setSpatialConceptType(SpatialConcept.INVERSEDISTANCE);

        return execute(inputFeatures, inputField);
    }

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField) {
        // Get input arguments, construct an "inputs" object

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

        // """Calculate General G and Z Score."""
        double dZSum = swMatrix.dZSum;
        double dZ2Sum = swMatrix.dZ2Sum;
        double dZ3Sum = swMatrix.dZ3Sum;
        double dZ4Sum = swMatrix.dZ4Sum;
        int lNumObs = swMatrix.Events.size();
        double rN = lNumObs * 1.0;

        double dZMean = swMatrix.dZSum / rN;
        double dZVar = Math.pow((swMatrix.dZ2Sum / rN) - Math.pow(dZMean, 2), 0.5);
        if (Math.abs(dZVar) <= 0.0) {
            LOGGER.warning("ERROR Zero variance:  all of the values for your input field are likely the same.");
        }

        // # Loop for each Z value.
        // int iNeighborCnt = 0;
        // int noNeighs = 0;
        // idsNoNeighs = [];
        double dNeighborProductSum = 0.0;
        double dTotalProductSum = 0.0;
        double dS1 = 0.0;
        double dS2 = 0.0;
        double dWijSum = 0.0;
        double dWijWji2Sum = 0.0;

        for (SpatialEvent curE : swMatrix.Events) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;

            for (SpatialEvent destE : swMatrix.Events) {
                if (curE.oid == destE.oid)
                    continue; // # i may not equal j

                dTotalProductSum += curE.weight * destE.weight;
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

                dNeighborProductSum += dWij * curE.weight * destE.weight;
                dWijSum += dWij;
                dWijWji2Sum += Math.pow(dWij + dWji, 2.0);
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
            }

            dS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
        }

        // #### Report if All Features Have No Neighbors ####
        if (Math.abs(dWijSum) <= 0) {
            LOGGER.log(Level.WARNING, "All features have no neighbors");
        }

        // #### Report on Features with No Neighbors ####

        // # Calculate B and S working variables needed to calculate variance.
        dS1 = 0.5 * dWijWji2Sum;
        double B0 = ((Math.pow(rN, 2.0) + (-3.0 * rN) + 3.0) * dS1) - (rN * dS2)
                + (3.0 * Math.pow(dWijSum, 2.0));
        double B1 = -1.0
                * (((Math.pow(rN, 2.0) - rN) * dS1) - (2.0 * rN * dS2) + (6.0 * Math.pow(dWijSum,
                        2.0)));
        double B2 = -1.0 * ((2.0 * rN * dS1) - ((rN + 3.0) * dS2) + (6.0 * Math.pow(dWijSum, 2.0)));
        double B3 = (4.0 * (rN - 1.0) * dS1) - (2.0 * (rN + 1.0) * dS2)
                + (8.0 * Math.pow(dWijSum, 2.0));
        double B4 = dS1 - dS2 + Math.pow(dWijSum, 2.0);

        // # Calculate Observed G, Expected G and Z Score.
        double dGObs = dNeighborProductSum / dTotalProductSum;
        double dGExp = dWijSum / (rN * (rN - 1.0));
        dZVar = ((((B0 * Math.pow(dZ2Sum, 2.0)) + (B1 * dZ4Sum)
                + (B2 * Math.pow(dZSum, 2.0) * dZ2Sum) + (B3 * dZSum * dZ3Sum) + (B4 * Math.pow(
                dZSum, 4.0))) / (Math.pow((Math.pow(dZSum, 2.0) - dZ2Sum), 2.0) * (rN * (rN - 1.0)
                * (rN - 2.0) * (rN - 3.0)))) - Math.pow(dGExp, 2.0));

        GeneralG generalG = new GeneralG(dGObs, dGExp, dZVar);
        generalG.setConceptualization(getSpatialConceptType());
        generalG.setDistanceMethod(getDistanceType());
        generalG.setRowStandardization(getStandardizationType());
        generalG.setDistanceThreshold(swMatrix.distanceBandWidth);

        return generalG;
    }

    public class GeneralG {
        // General G Summary
        // Observed General G: 0.000023
        // Expected General G: 0.000023
        // Variance: 0.000000
        // Z Score: -0.476909
        // p-value: 0.633427

        double observedGeneralG = 0.0;

        double expectedGeneralG = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.INVERSEDISTANCE;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.NONE;

        double distanceThreshold = 0.0;

        public GeneralG() {
        }

        public GeneralG(double observedGeneralG, double expectedGeneralG, double zVariance) {
            setObservedIndex(observedGeneralG);
            setExpectedIndex(expectedGeneralG);
            setZVariance(zVariance);
        }

        public void setObservedIndex(double observedGeneralG) {
            this.observedGeneralG = observedGeneralG;
        }

        public double getObservedIndex() {
            return observedGeneralG;
        }

        public void setExpectedIndex(double expectedGeneralG) {
            this.expectedGeneralG = expectedGeneralG;
        }

        public double getExpectedIndex() {
            return expectedGeneralG;
        }

        public void setZVariance(double zVariance) {
            this.zVariance = zVariance;
        }

        public double getZVariance() {
            return zVariance;
        }

        public double getZScore() {
            // dZScore = (dGObs - dGExp) / dZVar**0.5
            double dX = observedGeneralG - expectedGeneralG;
            double dY = Math.pow(getZVariance(), 0.5);
            if (dY == 0) {
                return 0.0;
            }

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
