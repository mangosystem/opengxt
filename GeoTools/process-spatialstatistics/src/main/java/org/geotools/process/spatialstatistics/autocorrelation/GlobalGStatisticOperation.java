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
 * Measures the degree of clustering for either high values or low values using the Getis-Ord General G statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalGStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGStatisticOperation.class);

    public GlobalGStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField,
            double distanceBand) {
        this.setDistanceBand(distanceBand);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);

        return execute(inputFeatures, inputField);
    }

    public GeneralG execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double wijSum = 0.0; // sum of all weights (wij)
        double tpSum = 0.0;
        double npSum = 0.0;
        double sumS1 = 0.0;
        double s2 = 0.0;

        double n = swMatrix.getEvents().size();

        for (SpatialEvent source : swMatrix.getEvents()) {
            double jwijSum = 0.0;
            double jwjiSum = 0.0;

            for (SpatialEvent target : swMatrix.getEvents()) {
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }

                tpSum += source.xVal * target.xVal;

                double wij = swMatrix.getWeight(source, target);
                double wji = wij;
                wij = swMatrix.standardizeWeight(source, wij);
                wji = swMatrix.standardizeWeight(target, wji);
                if (wij == 0) {
                    continue;
                }

                npSum += wij * source.xVal * target.xVal;

                wijSum += wij;
                sumS1 += Math.pow(wij + wji, 2.0);
                jwijSum += wij;
                jwjiSum += wji;
            }

            s2 += Math.pow(jwijSum + jwjiSum, 2.0);
        }

        // calculate B and S working variables needed to calculate variance.
        double s1 = 0.5 * sumS1;
        double wijSum2 = Math.pow(wijSum, 2.0);

        double b0 = ((Math.pow(n, 2.0) + (-3.0 * n) + 3.0) * s1) - (n * s2) + (3.0 * wijSum2);
        double b1 = -1.0 * (((Math.pow(n, 2.0) - n) * s1) - (2.0 * n * s2) + (6.0 * wijSum2));
        double b2 = -1.0 * ((2.0 * n * s1) - ((n + 3.0) * s2) + (6.0 * wijSum2));
        double b3 = (4.0 * (n - 1.0) * s1) - (2.0 * (n + 1.0) * s2) + (8.0 * wijSum2);
        double b4 = s1 - s2 + wijSum2;

        // Calculate Observed G, Expected G and Z Score.
        double dExpected = wijSum / (n * (n - 1.0));

        double sumX = swMatrix.sumX;
        double sumX2 = swMatrix.sumX2;
        double sumX3 = swMatrix.sumX3;
        double sumX4 = swMatrix.sumX4;
        double zVariance = (((b0 * Math.pow(sumX2, 2.0)) + (b1 * sumX4)
                + (b2 * Math.pow(sumX, 2.0) * sumX2) + (b3 * sumX * sumX3) + (b4 * Math.pow(sumX,
                4.0))) / (Math.pow((Math.pow(sumX, 2.0) - sumX2), 2.0) * (n * (n - 1.0) * (n - 2.0) * (n - 3.0))))
                - Math.pow(dExpected, 2.0);

        if (tpSum <= 0.0) {
            GeneralG generalG = new GeneralG(0d, dExpected, zVariance);
            generalG.setConceptualization(getSpatialConceptType());
            generalG.setDistanceMethod(getDistanceType());
            generalG.setRowStandardization(getStandardizationType());
            generalG.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return generalG;
        }

        double dObserved = npSum / tpSum;

        // finally build result
        GeneralG generalG = new GeneralG(dObserved, dExpected, zVariance);
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
