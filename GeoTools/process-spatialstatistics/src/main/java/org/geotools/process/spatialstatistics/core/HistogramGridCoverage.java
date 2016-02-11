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

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.logging.Logging;

/**
 * Histogram GridCoverage
 * 
 * @author Minpa Lee
 * @since 1.0
 * @version $Id: HistogramGridCoverage.java 1 2011-09-01 11:22:29Z minpa.lee $
 */
public class HistogramGridCoverage extends DataHistogram {
    protected static final Logger LOGGER = Logging.getLogger(HistogramGridCoverage.class);

    private GridCoverage2D coverage = null;

    private int bandIndex = 0;

    private double noData = 0.0d;

    @Override
    public boolean calculateHistogram(GridCoverage2D coverage, int bandIndex, double noData) {
        this.coverage = coverage;
        this.bandIndex = bandIndex;
        this.noData = noData;

        return calculate();
    }

    private boolean calculate() {
        count = 0;
        sumOfVals = 0;

        int bandCount = coverage.getNumSampleDimensions();
        if (bandIndex >= bandCount) {
            return false;
        }

        SortedMap<Double, Integer> valueCountsMap = new TreeMap<Double, Integer>();

        // 1. Iteration
        PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
        RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        readIter.startLines();
        while (!readIter.finishedLines()) {
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                double sampleValue = readIter.getSampleDouble(bandIndex);
                if (!SSUtils.compareDouble(noData, sampleValue)) {
                    if (valueCountsMap.containsKey(sampleValue)) {
                        int cnt = valueCountsMap.get(sampleValue);
                        valueCountsMap.put(sampleValue, new Integer(cnt + 1));
                    } else {
                        valueCountsMap.put(sampleValue, new Integer(1));
                    }

                    count++;
                    sumOfVals += sampleValue;
                }
                readIter.nextPixel();
            }
            readIter.nextLine();
        }

        if (valueCountsMap.size() == 0) {
            return false;
        }

        doubleArrayValues = new double[valueCountsMap.size()];
        longArrayFrequencies = new int[valueCountsMap.size()];

        Iterator<Double> iterator = valueCountsMap.keySet().iterator();
        int k = 0;
        while (iterator.hasNext()) {
            final double key = iterator.next();
            doubleArrayValues[k] = key;
            longArrayFrequencies[k] = valueCountsMap.get(key);
            k++;
        }

        return true;
    }

}
