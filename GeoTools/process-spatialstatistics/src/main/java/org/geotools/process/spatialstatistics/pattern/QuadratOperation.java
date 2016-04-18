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
package org.geotools.process.spatialstatistics.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Performs a point pattern analysis using quadrat method.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class QuadratOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(QuadratOperation.class);

    public QuadratResult execute(SimpleFeatureCollection features) {
        return execute(features, Double.valueOf(0d));
    }

    public QuadratResult execute(SimpleFeatureCollection features, Double cellSize) {
        QuadratResult result = new QuadratResult(features.getSchema().getTypeName());

        // 1. calculate extent and feature count
        Envelope bounds = null;
        List<Coordinate> coordinates = new ArrayList<Coordinate>();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                coordinates.add(geometry.getCentroid().getCoordinate());

                if (bounds == null) {
                    bounds = geometry.getEnvelopeInternal();
                } else {
                    bounds.expandToInclude(geometry.getEnvelopeInternal());
                }
            }
        } finally {
            featureIter.close();
        }

        // 2. prepare variables
        double area = bounds.getArea();
        if (cellSize == null || cellSize <= 0 || cellSize.isNaN() || cellSize.isInfinite()) {
            cellSize = Math.sqrt((area * 2) / coordinates.size());
        }

        double minX = bounds.getMinX();
        double minY = bounds.getMinY();
        int columns = (int) Math.ceil(bounds.getWidth() / cellSize);
        int rows = (int) Math.ceil(bounds.getHeight() / cellSize);

        // 3. calculate grids & count
        int iQuadratCount[][] = new int[rows][columns];
        for (Coordinate coordinate : coordinates) {
            int col = (int) Math.floor((coordinate.x - minX) / cellSize);
            int row = (int) Math.floor((coordinate.y - minY) / cellSize);
            iQuadratCount[row][col]++;
        }

        // 4. builds array
        final int quadrats[] = new int[rows * columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                quadrats[j + i * columns] = iQuadratCount[i][j];
            }
        }

        // 5. calculate statistics
        int quadratCount = quadrats.length;
        int maxVal = 0;
        double sumOfVals = 0;
        double sumOfSqrs = 0;

        for (int i = 0; i < quadratCount; i++) {
            sumOfVals += quadrats[i];
            sumOfSqrs += quadrats[i] * quadrats[i];
            maxVal = Math.max(maxVal, quadrats[i]);
        }

        double mean = sumOfVals / quadratCount;
        double variance = (sumOfSqrs - Math.pow(sumOfVals, 2.0) / quadratCount) / quadratCount;
        double VMR = variance / mean;

        // 6. test
        final int quadratsFreq[] = new int[maxVal + 1];
        for (int i = 0; i < quadratCount; i++) {
            quadratsFreq[quadrats[i]]++;
        }

        double dObsProb = 0d;
        double dPoissonProb = 0d;
        double dMaxDiff = 0d;
        for (int i = 0; i < quadratsFreq.length; i++) {
            dObsProb += (double) quadratsFreq[i] / (double) quadratCount;
            dPoissonProb += Math.pow(mean, i) * Math.exp(-mean) / factorial((double) i);
            dMaxDiff = Math.max(dMaxDiff, Math.abs(dObsProb - dPoissonProb));
        }

        double dKS = 1.36 / Math.sqrt(quadratCount);

        // finally, build result
        result.setArea(area);
        result.setFeatureCount(coordinates.size());
        result.setCellSize(cellSize);
        result.setColumns(columns);
        result.setRows(rows);

        result.setMean(mean);
        result.setVariance(variance);
        result.setVariance_Mean_Ratio(VMR);

        result.setNumber_of_Quadrats(quadratCount);
        result.setKolmogorov_Smirnov_Test(dMaxDiff);
        result.setCritical_Value_at_5percent(dKS);

        return result;
    }

    private double factorial(final double n) {
        double f = 1.0d;
        for (int i = 1; i <= n; i++) {
            f *= i;
        }
        return f;
    }

    public static final class QuadratResult {

        String typeName;

        int featureCount = 0;

        double area;

        double cellSize;

        int columns;

        int rows;

        int number_of_Quadrats;

        double mean = 0;

        double variance = 0;

        double variance_Mean_Ratio = 0;

        double kolmogorov_Smirnov_Test = 0;

        double critical_Value_at_5percent = 0;

        public QuadratResult(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public int getFeatureCount() {
            return featureCount;
        }

        public void setFeatureCount(int featureCount) {
            this.featureCount = featureCount;
        }

        public double getArea() {
            return area;
        }

        public void setArea(double area) {
            this.area = area;
        }

        public double getCellSize() {
            return cellSize;
        }

        public void setCellSize(double cellSize) {
            this.cellSize = cellSize;
        }

        public int getColumns() {
            return columns;
        }

        public void setColumns(int columns) {
            this.columns = columns;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getNumber_of_Quadrats() {
            return number_of_Quadrats;
        }

        public void setNumber_of_Quadrats(int number_of_Quadrats) {
            this.number_of_Quadrats = number_of_Quadrats;
        }

        public double getMean() {
            return mean;
        }

        public void setMean(double mean) {
            this.mean = mean;
        }

        public double getVariance() {
            return variance;
        }

        public void setVariance(double variance) {
            this.variance = variance;
        }

        public double getVariance_Mean_Ratio() {
            return variance_Mean_Ratio;
        }

        public void setVariance_Mean_Ratio(double variance_Mean_Ratio) {
            this.variance_Mean_Ratio = variance_Mean_Ratio;
        }

        public double getKolmogorov_Smirnov_Test() {
            return kolmogorov_Smirnov_Test;
        }

        public void setKolmogorov_Smirnov_Test(double kolmogorov_Smirnov_Test) {
            this.kolmogorov_Smirnov_Test = kolmogorov_Smirnov_Test;
        }

        public double getCritical_Value_at_5percent() {
            return critical_Value_at_5percent;
        }

        public void setCritical_Value_at_5percent(double critical_Value_at_5percent) {
            this.critical_Value_at_5percent = critical_Value_at_5percent;
        }
    }

}
