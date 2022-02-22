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
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * Extracts the cells of a raster based on a logical query.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExtractionOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterExtractionOperation.class);

    GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public GridCoverage2D execute(GridCoverage2D inputGc, Integer bandIndex, Filter filter) {
        if (filter == null || filter == Filter.INCLUDE) {
            return inputGc;
        }

        // prepare feature
        SimpleFeature feature = super.createTemplateFeature(inputGc);

        // create image
        RasterPixelType pixelType = RasterHelper.getTransferType(inputGc);
        DiskMemImage outputImage = this.createDiskMemImage(inputGc, pixelType);

        PlanarImage inputImage = (PlanarImage) inputGc.getRenderedImage();
        this.noData = RasterHelper.getNoDataValue(inputGc);
        GridTransformer trans = new GridTransformer(inputGc);

        java.awt.Rectangle inputBounds = inputImage.getBounds();
        RectIter inputIter = RectIterFactory.create(inputImage, inputBounds);
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        int row = 0; // inputBounds.y
        inputIter.startLines();
        writerIter.startLines();
        while (!inputIter.finishedLines() && !writerIter.finishedLines()) {
            int column = 0; // inputBounds.x
            inputIter.startPixels();
            writerIter.startPixels();
            while (!inputIter.finishedPixels() && !writerIter.finishedPixels()) {
                final double curVal = inputIter.getSampleDouble(bandIndex);

                if (SSUtils.compareDouble(curVal, this.noData)) {
                    writerIter.setSample(0, this.noData);
                } else {
                    Coordinate coord = trans.gridToWorldCoordinate(column, row);
                    feature.setDefaultGeometry(gf.createPoint(coord));

                    feature.setAttribute(1, curVal); // raster name
                    feature.setAttribute(2, curVal); // Value

                    if (filter.evaluate(feature)) {
                        writerIter.setSample(0, curVal);
                        updateStatistics(curVal);
                    } else {
                        writerIter.setSample(0, this.noData);
                    }
                }

                column++;
                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            row++;
            inputIter.nextLine();
            writerIter.nextLine();
        }

        return createGridCoverage(inputGc.getName(), outputImage);
    }
}
