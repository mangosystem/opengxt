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
package org.geotools.process.spatialstatistics.core;

import java.text.DecimalFormat;

import org.geotools.process.spatialstatistics.enumeration.StaticsType;

/**
 * Statistics Visitor Results
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsVisitorResult {

    // FIRST, LAST, SUM, MEAN, MIN, MAX, RANGE, STD, VAR, COUNT

    Object firstValue = null;

    Object lastValue = null;

    double sum = 0.0;

    double minimum = Double.MAX_VALUE;

    double maximum = Double.MIN_VALUE;

    double standardDeviation = 0.0;

    double variance = 0.0;

    int count = 0;

    int invalidCount = 0;

    Object noData;

    public Object getNoData() {
        return noData;
    }

    public void setNoData(Object noDataValue) {
        this.noData = noDataValue;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(Object firstValue) {
        this.firstValue = firstValue;
    }

    public Object getLastValue() {
        return lastValue;
    }

    public void setLastValue(Object lastValue) {
        this.lastValue = lastValue;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public double getMean() {
        if (count == 0 || sum == 0) {
            return 0.0;
        }
        return sum / count;
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public double getRange() {
        return count == 0 ? 0.0 : maximum - minimum;
    }

    public double getCoefficientOfVariance() {
        return count == 0 ? 0.0 : this.getStandardDeviation() / this.getMean();
    }

    public double getStandardDeviation() {
        return Math.sqrt(variance);
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(int invalidCount) {
        this.invalidCount = invalidCount;
    }

    public Object getValue(StaticsType resultType) {
        switch (resultType) {
        case Count:
            return getCount();
        case First:
            return getFirstValue();
        case Last:
            return getLastValue();
        case Maximum:
            return getMaximum();
        case Mean:
            return getMean();
        case Minimum:
            return getMinimum();
        case Range:
            return getRange();
        case StandardDeviation:
            return getStandardDeviation();
        case Sum:
            return getSum();
        case Variance:
            return getVariance();
        }
        return null;
    }

    @Override
    public String toString() {
        // FIRST, LAST, SUM, MEAN, MIN, MAX, RANGE, STD, VAR, COUNT
        StringBuffer sb = new StringBuffer();

        final DecimalFormat df = new DecimalFormat("##.######");
        sb.append("|| Statistics Summary").append("\r");
        sb.append("|| Count               ").append(df.format(getCount())).append("\r");
        sb.append("|| Sum                 ").append(df.format(getSum())).append("\r");
        sb.append("|| Mean                ").append(df.format(getMean())).append("\r");
        sb.append("|| Minimum             ").append(df.format(getMinimum())).append("\r");
        sb.append("|| Maximum             ").append(df.format(getMaximum())).append("\r");
        sb.append("|| Range               ").append(df.format(getRange())).append("\r");
        sb.append("|| Standard Deviation  ").append(df.format(getStandardDeviation())).append("\r");
        sb.append("|| Variance            ").append(df.format(getVariance())).append("\r");
        sb.append("|| First Value         ").append(df.format(getFirstValue())).append("\r");
        sb.append("|| Last  Value         ").append(df.format(getLastValue())).append("\r");

        return sb.toString();
    }
}
