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
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * Set Null sets identified cell locations to NoData based on a specified criteria.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSetNullOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterSetNullOperation.class);

    public RasterSetNullOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Integer bandIndex, Filter filter) {
        double noData = RasterHelper.getNoDataValue(inputCoverage);
        return execute(inputCoverage, bandIndex, filter, false, noData);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Integer bandIndex, Filter filter,
            boolean replaceNoData, double newValue) {
        // must be validation filter
        if (filter == null || filter == Filter.INCLUDE || filter == Filter.EXCLUDE) {
            return inputCoverage;
        }

        this.noData = RasterHelper.getNoDataValue(inputCoverage);

        // decide pixel type
        RasterPixelType pixelType = RasterHelper.getTransferType(inputCoverage);
        SimpleFeature feature = this.createTemplateFeature(inputCoverage);

        PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        DiskMemImage outputImage = this.createDiskMemImage(inputCoverage, pixelType);
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        inputIter.startLines();
        writerIter.startLines();
        while (!inputIter.finishedLines() && !writerIter.finishedLines()) {
            inputIter.startPixels();
            writerIter.startPixels();
            while (!inputIter.finishedPixels() && !writerIter.finishedPixels()) {
                double value = inputIter.getSampleDouble(bandIndex);

                // check nodata
                boolean isNoData = SSUtils.compareDouble(value, noData);
                if (isNoData && replaceNoData) {
                    writerIter.setSample(0, newValue);
                    this.updateStatistics(value);
                    inputIter.nextPixel();
                    writerIter.nextPixel();
                    continue;
                }

                if (isNoData) {
                    writerIter.setSample(0, noData);
                } else {
                    // evaluate grid value
                    feature.setAttribute(1, value); // raster name
                    feature.setAttribute(2, value); // Value
                    if (filter.evaluate(feature)) {
                        // set null
                        writerIter.setSample(0, noData);
                    } else {
                        writerIter.setSample(0, value);
                        this.updateStatistics(value);
                    }
                }

                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            inputIter.nextLine();
            writerIter.nextLine();
        }

        return createGridCoverage(inputCoverage.getName(), outputImage);
    }
}
