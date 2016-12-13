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

    public GlobalGearysCOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public GearysC execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double cijWSum = 0.0;
        double wijSum = 0.0; // sum of all weights (wij)
        double m2 = 0.0;
        double m4 = 0.0;
        double sumS1 = 0.0;
        double sumS2 = 0.0;

        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;

        for (SpatialEvent source : swMatrix.getEvents()) {
            double jwijSum = 0.0;
            double jwjiSum = 0.0;

            double zi = source.xVal - meanX;
            m2 += Math.pow(zi, 2.0);
            m4 += Math.pow(zi, 4.0);

            for (SpatialEvent target : swMatrix.getEvents()) {
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }

                double wij = swMatrix.getWeight(source, target);
                double wji = wij;
                wij = swMatrix.standardizeWeight(source, wij);
                wji = swMatrix.standardizeWeight(target, wji);
                if (wij == 0) {
                    continue;
                }

                double cij = Math.pow(source.xVal - target.xVal, 2.0);

                cijWSum += wij * cij;
                wijSum += wij;
                jwijSum += wij;
                jwjiSum += wji;
                sumS1 += Math.pow(wij + wji, 2.0);
            }

            sumS2 += Math.pow(jwijSum + jwjiSum, 2.0);
        }

        double dS1 = 0.5 * sumS1;

        m2 = m2 / (n - 1.0);
        m4 = m4 / (n - 1.0);

        // Geary’s c ranges from zero to infinity, with an expected value of 1 under no autocorrelation.
        // Values from zero to one indicate positive spatial autocorrelation,
        // values above 1 negative spatial autocorrelation.
        double b2 = m4 / (m2 * m2);
        double dExpected = 1.0;

        if (wijSum <= 0.0) {
            GearysC gearysC = new GearysC(0d, dExpected, 0d);
            gearysC.setConceptualization(getSpatialConceptType());
            gearysC.setDistanceMethod(getDistanceType());
            gearysC.setRowStandardization(getStandardizationType());
            gearysC.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return gearysC;
        }

        double dObserved = cijWSum / (2.0 * m2 * wijSum);

        // variance of c
        double W2 = Math.pow(wijSum, 2.0);
        double n2 = Math.pow(n, 2.0);
        double div = n * (n - 2.0) * (n - 3.0);
        double A = ((n - 1) * dS1 * (n2 - (3.0 * n) + 3.0 - ((n - 1) * b2))) / (div * W2);
        double B = ((n - 1) * sumS2 * (n2 + (3.0 * n) - 6.0 - ((n2 - n + 2) * b2)))
                / (4.0 * div * W2);
        double C = (n2 - 3.0 - (Math.pow(n - 1, 2.0) * b2)) / div;

        double zVariance = A - B + C;

        // finally build result
        GearysC gearysC = new GearysC(dObserved, dExpected, zVariance);
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
