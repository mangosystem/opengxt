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
import org.geotools.process.spatialstatistics.core.DataHistogram;
import org.geotools.process.spatialstatistics.core.HistogramFeatures;
import org.geotools.process.spatialstatistics.core.HistogramGridCoverage;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.logging.Logging;

/**
 * The Quantile creates an equal (or close to equal) number of values in each class. For example, if there were 12 values, then three classes would
 * represent four values each.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class QuantileClassify extends DataClassify {
    protected static final Logger LOGGER = Logging.getLogger(QuantileClassify.class);

    static final String sMethodName = "Quantile";

    @Override
    public String getMethodName() {
        return sMethodName;
    }

    @Override
    public Double[] classify(double[] doubleValues, int[] longFrequencies, int binCount) {
        initializeClassBreaks(binCount);

        if (doubleValues == null || longFrequencies == null) {
            return classBreaks;
        }

        // calculate sample count
        final int arrayCount = longFrequencies.length;
        int sampleCount = 0;
        for (int index = 0; index < arrayCount; index++) {
            sampleCount += longFrequencies[index];
        }

        calculate(doubleValues, longFrequencies, sampleCount, binCount);

        return classBreaks;
    }

    @Override
    public Double[] classify(SimpleFeatureCollection inputFeatures, String propertyName,
            int binCount) {
        initializeClassBreaks(binCount);

        DataHistogram histo = new HistogramFeatures();
        if (histo.calculateHistogram(inputFeatures, propertyName)) {
            final int sampleCount = histo.getCount();
            final double[] arrayValues = histo.getArrayValues();
            final int[] arrayFrequencies = histo.getArrayFrequencies();

            calculate(arrayValues, arrayFrequencies, sampleCount, binCount);
        }

        return classBreaks;
    }

    @Override
    public Double[] classify(GridCoverage2D inputGc, int bandIndex, int binCount) {
        initializeClassBreaks(binCount);

        final double noDataValue = RasterHelper.getNoDataValue(inputGc);
        DataHistogram histo = new HistogramGridCoverage();
        if (histo.calculateHistogram(inputGc, bandIndex, noDataValue)) {
            final int sampleCount = histo.getCount();
            final double[] arrayValues = histo.getArrayValues();
            final int[] arrayFrequencies = histo.getArrayFrequencies();

            calculate(arrayValues, arrayFrequencies, sampleCount, binCount);
        }

        return classBreaks;
    }

    private void calculate(double[] arrayValues, int[] arrayFrequencies, int sampleCount,
            int binCount) {
        if (binCount >= arrayValues.length) {
            classBreaks = new Double[arrayValues.length];
            for (int index = 0; index < arrayValues.length; index++) {
                classBreaks[index] = arrayValues[index];
            }
            return;
        }

        classBreaks[0] = Double.valueOf(arrayValues[0]);
        classBreaks[binCount] = Double.valueOf(arrayValues[arrayValues.length - 1]);

        double[] mdaValInit = new double[binCount - 1];
        final double quanta = (double) sampleCount / (double) binCount;

        int frequencies = 0;
        int posIndex = 0;
        for (int binIndex = 1; binIndex < binCount; binIndex++) {
            final int val = (int) Math.ceil(quanta * binIndex);

            for (int valueIndex = posIndex; valueIndex < arrayValues.length; valueIndex++) {
                frequencies += arrayFrequencies[valueIndex];

                if (val <= frequencies) {
                    classBreaks[binIndex] = Double.valueOf(arrayValues[valueIndex]);
                    posIndex = valueIndex + 1;
                    if (posIndex < arrayValues.length) {
                        mdaValInit[binIndex - 1] = arrayValues[posIndex];
                    } else {
                        mdaValInit[binIndex - 1] = arrayValues[valueIndex];
                    }
                    break;
                }
            }
        }
    }
}
