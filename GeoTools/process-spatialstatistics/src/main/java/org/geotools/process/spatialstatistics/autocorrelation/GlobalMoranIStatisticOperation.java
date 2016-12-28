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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Moran's I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalMoranIStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(GlobalMoranIStatisticOperation.class);

    public GlobalMoranIStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(false);
    }

    public MoransI execute(SimpleFeatureCollection inputFeatures, String inputField) {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        double zijWSum = 0.0;
        double wijSum = 0.0; // sum of all weights (wij)
        double ziSum2 = 0.0;
        double ziSum4 = 0.0;
        double sumS1 = 0.0;
        double sumS2 = 0.0;

        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;

        for (SpatialEvent source : swMatrix.getEvents()) {
            double jwijSum = 0.0;
            double jwjiSum = 0.0;

            double zi = source.xVal - meanX;
            ziSum2 += Math.pow(zi, 2.0);
            ziSum4 += Math.pow(zi, 4.0);

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

                double zj = target.xVal - meanX;

                zijWSum += wij * zi * zj;
                wijSum += wij;
                jwijSum += wij;
                jwjiSum += wji;
                sumS1 += Math.pow(wij + wji, 2.0);
            }

            sumS2 += Math.pow(jwijSum + jwjiSum, 2.0);
        }

        ziSum2 = ziSum2 / n; // standard deviation
        ziSum4 = ziSum4 / n;

        double dExpected = -1.0 / (n - 1.0);

        if (wijSum <= 0.0) {
            MoransI moransI = new MoransI(0d, dExpected, 0d);
            moransI.setConceptualization(getSpatialConceptType());
            moransI.setDistanceMethod(getDistanceType());
            moransI.setRowStandardization(getStandardizationType());
            moransI.setDistanceThreshold(swMatrix.getDistanceBandWidth());
            return moransI;
        }

        double dObserved = zijWSum / (ziSum2 * wijSum);

        double s1 = 0.5 * sumS1;
        double wijSum2 = Math.pow(wijSum, 2.0);
        double b2 = ziSum4 / (ziSum2 * ziSum2); // sample kurtosis
        double a = n * ((Math.pow(n, 2.0) - (3.0 * n) + 3.0) * s1 - (n * sumS2) + 3.0 * wijSum2);
        double b = b2 * ((Math.pow(n, 2.0) - n) * s1 - (2.0 * n * sumS2) + 6.0 * wijSum2);
        double c = ((n - 1.0) * (n - 2.0) * (n - 3.0) * (Math.pow(wijSum, 2.0)));

        double zVariance = (a / c) - (b / c) - Math.pow(dExpected, 2.0);

        // finally build result
        MoransI moransI = new MoransI(dObserved, dExpected, zVariance);
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
