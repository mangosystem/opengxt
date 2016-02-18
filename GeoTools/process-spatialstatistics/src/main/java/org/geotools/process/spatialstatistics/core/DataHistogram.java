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

import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/**
 * Abstract Data Histogram
 * 
 * @author Minpa Lee
 * @since 1.0
 * @version $Id: DataHistogram.java 1 2011-09-01 11:22:29Z minpa.lee $
 */
public abstract class DataHistogram {
    protected static final Logger LOGGER = Logging.getLogger(DataHistogram.class);

    private long maxSampleSize = 999999;

    private int missingValueCount = 0;

    protected int count = 0;

    protected double sumOfVals = 0;

    String normalProperty = null;

    public int getCount() {
        return count;
    }

    public double getMean() {
        return count == 0 ? 0 : sumOfVals / count;
    }

    public long getMaxSampleSize() {
        return maxSampleSize;
    }

    public void setMaxSampleSize(long sampleSize) {
        this.maxSampleSize = sampleSize;
    }

    public long getMissingValueCount() {
        return missingValueCount;
    }

    public String getNormalProperty() {
        return normalProperty;
    }

    public void setNormalProperty(String normalField) {
        this.normalProperty = normalField;
    }

    protected double[] doubleArrayValues;

    protected int[] longArrayFrequencies;

    public double[] getArrayValues() {
        return this.doubleArrayValues;
    }

    public int[] getArrayFrequencies() {
        return this.longArrayFrequencies;
    }

    public boolean calculateHistogram(SimpleFeatureSource srcFc, Filter filter, String attributeName) {
        return false;
    }

    public boolean calculateHistogram(SimpleFeatureCollection srcFc, String attributeName) {
        return false;
    }

    public boolean calculateHistogram(GridCoverage2D srcCoverage, int bandIndex, double noData) {
        return false;
    }
}
