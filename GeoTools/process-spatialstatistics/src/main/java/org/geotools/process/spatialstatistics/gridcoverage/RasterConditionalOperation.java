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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Literal;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * Performs a conditional if/else evaluation on each of the input cells of an input raster.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterConditionalOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterConditionalOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputGc, Integer bandIndex, Filter inputFilter) {
        return execute(inputGc, bandIndex, inputFilter, Integer.valueOf(1), Integer.MIN_VALUE);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, Integer bandIndex, Filter inputFilter,
            Integer trueValue) {
        return execute(inputGc, bandIndex, inputFilter, trueValue, Integer.MIN_VALUE);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, Integer bandIndex, Filter filter,
            Integer trueValue, Integer falseValue) {
        // check parameters
        if (filter == null || filter == Filter.INCLUDE) {
            return inputGc;
        }

        // decide pixel type
        final boolean isNoDataFilter = isNodataFilter(filter);
        final double inputNoData = RasterHelper.getNoDataValue(inputGc);

        final int valueTrue = trueValue.intValue();
        final int valueFalse = falseValue.intValue();
        SimpleFeature feature = this.createTemplateFeature(inputGc);

        PlanarImage inputImage = (PlanarImage) inputGc.getRenderedImage();
        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        DiskMemImage outputImage = this.createDiskMemImage(inputGc, RasterPixelType.INTEGER);
        this.noData = Integer.MIN_VALUE;  // change nodata value
        
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        inputIter.startLines();
        writerIter.startLines();
        while (!inputIter.finishedLines() && !writerIter.finishedLines()) {
            inputIter.startPixels();
            writerIter.startPixels();
            while (!inputIter.finishedPixels() && !writerIter.finishedPixels()) {
                double curVal = inputIter.getSampleDouble(bandIndex);

                if (isNoDataFilter) {
                    if (SSUtils.compareDouble(curVal, inputNoData)) {
                        writerIter.setSample(0, valueTrue);
                    } else {
                        writerIter.setSample(0, valueFalse);
                    }
                } else {
                    if (SSUtils.compareDouble(curVal, inputNoData)) {
                        writerIter.setSample(0, this.noData);
                    } else {
                        feature.setAttribute(1, curVal); // raster name
                        feature.setAttribute(2, curVal); // Value

                        int conValue = filter.evaluate(feature) ? valueTrue : valueFalse;
                        writerIter.setSample(0, conValue);
                    }
                }

                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            inputIter.nextLine();
            writerIter.nextLine();
        }

        if (SSUtils.compareDouble(valueFalse, this.noData)) {
            minValue = valueTrue;
        } else {
            minValue = Math.min(valueTrue, valueFalse);
        }

        maxValue = Math.max(valueTrue, valueFalse);

        return createGridCoverage(inputGc.getName(), outputImage);
    }

    private boolean isNodataFilter(Filter filter) {
        if (filter instanceof BinaryComparisonOperator) {
            BinaryComparisonOperator bcOp = (BinaryComparisonOperator) filter;
            Literal literal = (Literal) bcOp.getExpression2();
            return literal.toString().equalsIgnoreCase("NoData");
        }
        return false;
    }
}
