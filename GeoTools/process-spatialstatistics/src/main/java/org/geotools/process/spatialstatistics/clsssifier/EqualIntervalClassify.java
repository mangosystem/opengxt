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

import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DataStatistics;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.util.logging.Logging;

/**
 * The EqualInterval subdivides the data range by the number of classes to produce the equal value intervals for each class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class EqualIntervalClassify extends DataClassify {
    protected static final Logger LOGGER = Logging.getLogger(EqualIntervalClassify.class);

    static final String sMethodName = "Equal Interval";

    @Override
    public String getMethodName() {
        return sMethodName;
    }

    @Override
    public Double[] classify(double[] doubleArrayValues, int[] longArrayFrequencies, int binCount) {
        initializeClassBreaks(binCount);

        if (doubleArrayValues == null || longArrayFrequencies == null) {
            return classBreaks;
        }

        final int arrayCount = doubleArrayValues.length;
        calculate(doubleArrayValues[0], doubleArrayValues[arrayCount - 1], binCount);

        return classBreaks;
    }

    @Override
    public Double[] classify(SimpleFeatureCollection inputFeatures, String propertyName,
            int binCount) {
        initializeClassBreaks(binCount);

        DataStatistics dataStatistics = new DataStatistics();
        StatisticsVisitorResult statistics = dataStatistics.getStatistics(inputFeatures,
                propertyName);
        if (statistics.getCount() > 0) {
            calculate(statistics.getMinimum(), statistics.getMaximum(), binCount);
        }

        return classBreaks;
    }

    @Override
    public Double[] classify(GridCoverage2D inputGc, int bandIndex, int binCount) {
        initializeClassBreaks(binCount);

        DataStatistics dataStatistics = new DataStatistics();
        StatisticsVisitorResult statistics = dataStatistics.getStatistics(inputGc, bandIndex);
        if (statistics.getCount() > 0) {
            calculate(statistics.getMinimum(), statistics.getMaximum(), binCount);
        }

        return classBreaks;
    }

    private void calculate(double minValue, double maxValue, int binCount) {
        classBreaks = new Double[binCount + 1];

        final double interval = (maxValue - minValue) / binCount;

        // the first break is the minimum value
        classBreaks[0] = minValue;

        for (int k = 1; k < binCount; k++) {
            final double val = FormatUtils.round(minValue + (k * interval), 6);
            classBreaks[k] = Double.valueOf(val);
        }

        // the last break is the maximum value
        classBreaks[binCount] = maxValue;
    }
}
