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
package org.geotools.process.spatialstatistics.clsssifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DataStatistics;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.util.logging.Logging;

/**
 * The StandardDeviation represents dispersion about the mean, and this classification creates classes that represent this dispersion.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDeviationClassify extends DataClassify {
    protected static final Logger LOGGER = Logging.getLogger(StandardDeviationClassify.class);

    static final String sMethodName = "Standard Deviation";

    double mean = Double.NaN;

    double standardDev = Double.NaN;

    double interval = 1.0;

    @Override
    public String getMethodName() {
        return sMethodName;
    }

    /**
     * The mean value.
     * 
     * @param mean
     */
    public void setMean(double mean) {
        this.mean = mean;
    }

    /**
     * The deviation interval (1/4 <= value <= 1).
     * 
     * @param stdDev
     */
    public void setStandardDev(double stdDev) {
        this.standardDev = stdDev;
    }

    /**
     * The deviation interval (1/4 <= value <= 1).
     * 
     * @param stdDev
     */
    public void setDeviationInterval(double interval) {
        if (interval > 1) {
            interval = 1.0;
        }
        this.interval = interval;
    }

    @Override
    public Double[] classify(double[] doubleArrayValues, int[] longArrayFrequencies, int binCount) {
        initializeClassBreaks(binCount);

        if (doubleArrayValues == null || longArrayFrequencies == null) {
            return classBreaks;
        }

        StatisticsVisitorResult statistics = updateStatistics(doubleArrayValues,
                longArrayFrequencies);

        calculate(statistics, binCount);

        return classBreaks;
    }

    @Override
    public Double[] classify(SimpleFeatureCollection inputFeatures, String propertyName,
            int binCount) {
        DataStatistics dataStatistics = new DataStatistics();
        StatisticsVisitorResult statistics = dataStatistics.getStatistics(inputFeatures,
                propertyName);

        initializeClassBreaks(binCount);

        calculate(statistics, binCount);

        return classBreaks;
    }

    @Override
    public Double[] classify(GridCoverage2D inputGc, int bandIndex, int binCount) {
        DataStatistics dataStatistics = new DataStatistics();
        StatisticsVisitorResult statistics = dataStatistics.getStatistics(inputGc, bandIndex);

        initializeClassBreaks(binCount);

        calculate(statistics, binCount);

        return classBreaks;
    }

    private void calculate(StatisticsVisitorResult statistics, int binCount) {
        if (statistics.getCount() == 0) {
            return;
        }

        final double minValue = statistics.getMinimum();
        final double maxValue = statistics.getMaximum();

        this.mean = statistics.getMean();
        this.standardDev = statistics.getStandardDeviation();

        // Mean is 1000, sd is 3000, breaks are set at 2500 5500, 8500
        List<Double> stdClassBreaks = new ArrayList<Double>();

        // =======================================
        // http://en.wikipedia.org/wiki/Standard_deviation
        // http://en.wikipedia.org/wiki/68-95-99.7_rule : 0.6827, 0.9545, 0.9973
        // 1 : 0.5, 1.5, 2.5
        // 1/2 : 0.25, 0.75, 1.2, 1.7, 2.3, 2.8
        // 1/3 : 0.17, 0.50, 0.83, 1.2, 1.5, 1.8, 2.2, 2.5, 2.8
        // 1/4 : 0.13, 0.38, 0.63, 0.88, 1.1, 1.4, 1.6, 1.9, 2.1, 2.4, 2.6, 2.9
        // =======================================

        final double zScore = standardDev * interval;
        final double cutOffValue = mean + (zScore / 2.0);

        if (cutOffValue > minValue && cutOffValue < maxValue) {
            stdClassBreaks.add(Double.valueOf(minValue));
            stdClassBreaks.add(Double.valueOf(cutOffValue));
            stdClassBreaks.add(Double.valueOf(maxValue));
        }

        final int maxInterval = (int) Math.floor(1.0 / this.interval);
        int max = 0;
        int exp = 1;
        while (true) {
            final double expVal = zScore * exp++;
            final double highValue = cutOffValue + expVal;
            final double lowValue = cutOffValue - expVal;

            final boolean isHigh = highValue < maxValue ? true : false;
            final boolean isLow = lowValue > minValue ? true : false;

            if (isHigh) {
                stdClassBreaks.add(Double.valueOf(highValue));
            }

            if (isLow) {
                stdClassBreaks.add(Double.valueOf(lowValue));
            }

            if (!isHigh && !isLow) {
                break;
            } else if (!isHigh || !isLow) {
                max++;
                if (max == maxInterval) {
                    break;
                }
            }
        }

        Collections.sort(stdClassBreaks);

        classBreaks = new Double[stdClassBreaks.size()];
        stdClassBreaks.toArray(classBreaks);
    }

    private StatisticsVisitorResult updateStatistics(double[] doubleArrayValues,
            int[] longArrayFrequencies) {
        StatisticsVisitor visitor = new StatisticsVisitor(new DoubleStrategy());
        final int arrayCount = doubleArrayValues.length;
        for (int k = 0; k < arrayCount; k++) {
            for (int i = 0; i < longArrayFrequencies[k]; i++) {
                visitor.visit(doubleArrayValues[k]);
            }
        }
        return visitor.getResult();
    }
}
