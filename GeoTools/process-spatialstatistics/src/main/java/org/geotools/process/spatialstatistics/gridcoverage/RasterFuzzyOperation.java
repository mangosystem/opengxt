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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.FuzzyDirectionType;
import org.geotools.process.spatialstatistics.enumeration.FuzzyFunctionType;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * Performs fuzzy membership function on rasters.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFuzzyOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterFuzzyOperation.class);

    static final double HALF_PIE = Math.PI / 2.0;

    private FuzzyFunctionType functionType = FuzzyFunctionType.Linear;

    private FuzzyDirectionType direction = FuzzyDirectionType.Decreasing;

    private double maxRange = 1.0;

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void setFunctionType(FuzzyFunctionType functionType) {
        this.functionType = functionType;
    }

    public FuzzyFunctionType getFunctionType() {
        return this.functionType;
    }

    public void setDirection(FuzzyDirectionType direction) {
        this.direction = direction;
    }

    public FuzzyDirectionType getDirection() {
        return this.direction;
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, double minValue, double maxValue,
            double midValue1, double midValue2) {
        double[] fuzzyValues = { minValue, midValue1, midValue2, maxValue };
        return execute(inputGc, fuzzyValues);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, double[] fuzzyValues) {
        DiskMemImage outputImage = this.createDiskMemImage(inputGc, RasterPixelType.FLOAT);

        PlanarImage inputImage = (PlanarImage) inputGc.getRenderedImage();
        final double inputNoData = RasterHelper.getNoDataValue(inputGc);
        this.noData = RasterHelper.getDefaultNoDataValue(pixelType);

        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        inputIter.startLines();
        writerIter.startLines();
        while (!inputIter.finishedLines() && !writerIter.finishedLines()) {
            inputIter.startPixels();
            writerIter.startPixels();
            while (!inputIter.finishedPixels() && !writerIter.finishedPixels()) {
                final double inputVal = inputIter.getSampleDouble(0);

                if (SSUtils.compareDouble(inputNoData, inputVal)) {
                    writerIter.setSample(0, noData);
                } else {
                    double fuzzyValue = getFuzzyValue(inputVal, fuzzyValues) * maxRange;
                    writerIter.setSample(0, fuzzyValue);
                    updateStatistics(inputVal);
                }

                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            inputIter.nextLine();
            writerIter.nextLine();
        }

        return createGridCoverage("Fuzzy", outputImage);
    }

    private double getFuzzyValue(double curVal, double[] fuzzyValues) {
        double fuzzyVal = 0.0;

        double minValue = fuzzyValues[0];
        double midValue1 = fuzzyValues[1];
        double midValue2 = fuzzyValues[2];
        double maxValue = fuzzyValues[3];

        int validVariable = 4;
        validVariable = Double.isNaN(midValue2) ? 3 : validVariable;
        validVariable = Double.isNaN(midValue1) ? 2 : validVariable;

        double dX;
        double dW;

        if (validVariable == 3) {
            // midValue2 == NaN
            if (curVal == midValue1) {
                fuzzyVal = 1;
            } else if (curVal <= minValue || curVal >= maxValue) {
                fuzzyVal = 0;
            } else if (curVal > minValue && curVal < midValue1) {
                // Increasing
                dX = curVal - minValue;
                dW = midValue1 - minValue;
                fuzzyVal = getValue(dX, dW);
            } else if (curVal > midValue1 && curVal < maxValue) {
                // Decreasing
                dX = curVal - midValue1;
                dW = maxValue - midValue1;
                fuzzyVal = 1 - getValue(dX, dW);
            }
        } else if (validVariable == 4) {
            if (curVal >= midValue1 && curVal <= midValue2) {
                fuzzyVal = 1;
            } else if (curVal <= minValue || curVal >= maxValue) {
                fuzzyVal = 0;
            } else if (curVal > minValue && curVal < midValue1) {
                // Increasing
                dX = curVal - minValue;
                dW = midValue1 - minValue;
                fuzzyVal = getValue(dX, dW);
            } else if (curVal > midValue2 && curVal < maxValue) {
                // Decreasing
                dX = maxValue - midValue2;
                dW = curVal - midValue2;
                fuzzyVal = 1 - getValue(dX, dW);
            }
        } else {
            // midValue1, midValue2 == NaN
            if (maxValue == minValue) {
                fuzzyVal = 1;
            } else if (curVal <= minValue) {
                fuzzyVal = 0;
            } else if (curVal >= maxValue) {
                fuzzyVal = 1;
            } else {
                dX = curVal - minValue;
                dW = maxValue - minValue;
                fuzzyVal = getValue(dX, dW);
            }
        }

        if (direction == FuzzyDirectionType.Decreasing) {
            fuzzyVal = 1 - fuzzyVal;
        }
        return fuzzyVal;
    }

    private double getValue(double dX, double dW) {
        double fuzzyVal = 0;
        if (dW == 0) {
            return fuzzyVal;
        }

        switch (functionType) {
        case Linear:
            fuzzyVal = dX / dW;
            break;
        case Sigmoidal:
            fuzzyVal = Math.pow(Math.sin((dX / dW) * HALF_PIE), 2.0);
            break;
        case Jshaped:
            fuzzyVal = 1.0 / (1.0 + Math.pow((dW - dX) / dW, 2.0));
            break;
        }

        return fuzzyVal;
    }
}