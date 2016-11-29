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
 * Measures the degree of clustering for either high values or low values using the Getis-Ord General G statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalGStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGStatisticOperation.class);

    private SpatialWeightMatrix swMatrix = null;

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField,
            double distanceBand) {
        this.setDistanceBand(distanceBand);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);

        return execute(inputFeatures, inputField);
    }

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField) {
        // Get input arguments, construct an "inputs" object

        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double dZSum = swMatrix.dZSum;
        double dZ2Sum = swMatrix.dZ2Sum;
        double dZ3Sum = swMatrix.dZ3Sum;
        double dZ4Sum = swMatrix.dZ4Sum;
        double n = swMatrix.getEvents().size() * 1.0;

        double dZMean = swMatrix.dZSum / n;
        double dZVar = Math.pow((swMatrix.dZ2Sum / n) - Math.pow(dZMean, 2), 0.5);

        double dNeighborProductSum = 0.0;
        double dTotalProductSum = 0.0;
        double dS1 = 0.0;
        double dS2 = 0.0;
        double dWijSum = 0.0;
        double dWijWji2Sum = 0.0;

        for (SpatialEvent curE : swMatrix.getEvents()) {
            double dWijS2Sum = 0.0;
            double dWjiS2Sum = 0.0;

            for (SpatialEvent destE : swMatrix.getEvents()) {
                if (curE.oid == destE.oid)
                    continue;

                dTotalProductSum += curE.weight * destE.weight;

                // Calculate the weight (dWij)
                double dWij = swMatrix.getWeight(curE, destE);
                double dWji = dWij;

                if (getStandardizationType() == StandardizationMethod.Row) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                    dWji = swMatrix.standardizeWeight(destE, dWji);
                }

                dNeighborProductSum += dWij * (curE.weight * destE.weight);
                dWijSum += dWij;
                dWijWji2Sum += Math.pow(dWij + dWji, 2.0);
                dWijS2Sum += dWij;
                dWjiS2Sum += dWji;
            }

            dS2 += Math.pow(dWijS2Sum + dWjiS2Sum, 2.0);
        }

        // Calculate B and S working variables needed to calculate variance.
        dS1 = 0.5 * dWijWji2Sum;
        double B0 = ((Math.pow(n, 2.0) + (-3.0 * n) + 3.0) * dS1) - (n * dS2)
                + (3.0 * Math.pow(dWijSum, 2.0));
        double B1 = -1.0
                * (((Math.pow(n, 2.0) - n) * dS1) - (2.0 * n * dS2) + (6.0 * Math.pow(dWijSum, 2.0)));
        double B2 = -1.0 * ((2.0 * n * dS1) - ((n + 3.0) * dS2) + (6.0 * Math.pow(dWijSum, 2.0)));
        double B3 = (4.0 * (n - 1.0) * dS1) - (2.0 * (n + 1.0) * dS2)
                + (8.0 * Math.pow(dWijSum, 2.0));
        double B4 = dS1 - dS2 + Math.pow(dWijSum, 2.0);

        // Calculate Observed G, Expected G and Z Score.
        double dGExp = dWijSum / (n * (n - 1.0));
        dZVar = ((((B0 * Math.pow(dZ2Sum, 2.0)) + (B1 * dZ4Sum)
                + (B2 * Math.pow(dZSum, 2.0) * dZ2Sum) + (B3 * dZSum * dZ3Sum) + (B4 * Math.pow(
                dZSum, 4.0))) / (Math.pow((Math.pow(dZSum, 2.0) - dZ2Sum), 2.0) * (n * (n - 1.0)
                * (n - 2.0) * (n - 3.0)))) - Math.pow(dGExp, 2.0));

        if (dTotalProductSum <= 0.0) {
            GeneralG generalG = new GeneralG(0d, dGExp, dZVar);
            generalG.setConceptualization(getSpatialConceptType());
            generalG.setDistanceMethod(getDistanceType());
            generalG.setRowStandardization(getStandardizationType());
            generalG.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return generalG;
        }

        double dGObs = dNeighborProductSum / dTotalProductSum;

        GeneralG generalG = new GeneralG(dGObs, dGExp, dZVar);
        generalG.setConceptualization(getSpatialConceptType());
        generalG.setDistanceMethod(getDistanceType());
        generalG.setRowStandardization(getStandardizationType());
        generalG.setDistanceThreshold(swMatrix.getDistanceBandWidth());

        return generalG;
    }

    public static final class GeneralG {

        double observedGeneralG = 0.0;

        double expectedGeneralG = 0.0;

        double zVariance = 0.0;

        SpatialConcept conceptualization = SpatialConcept.InverseDistance;

        DistanceMethod distanceMethod = DistanceMethod.Euclidean;

        StandardizationMethod rowStandardization = StandardizationMethod.None;

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
            double dX = observedGeneralG - expectedGeneralG;
            double dY = Math.pow(getZVariance(), 0.5);
            if (dY == 0) {
                return 0.0;
            }

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
