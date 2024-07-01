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

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * Extracts the cells of a raster that correspond to the areas defined by a mask.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExtractByMaskOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterExtractByMaskOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputCoverage, GridCoverage2D maskCoverage) {
        noData = RasterHelper.getNoDataValue(inputCoverage);

        GridGeometry2D gridGeometry2D = inputCoverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        pixelSizeX = Math.abs(gridToWorld.getScaleX());
        pixelSizeY = Math.abs(gridToWorld.getScaleY());

        CoordinateReferenceSystem inputCrs = inputCoverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem maskCrs = maskCoverage.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(inputCrs, maskCrs)) {
            // Reproject raster
            RasterReprojectOperation reprojectOp = new RasterReprojectOperation();
            maskCoverage = reprojectOp.execute(maskCoverage, inputCrs, ResampleType.BILINEAR,
                    pixelSizeX, pixelSizeY);
        }

        // init
        final double maskNoData = RasterHelper.getNoDataValue(maskCoverage);

        // resample mask coverage

        GridGeometry2D gg2D = maskCoverage.getGridGeometry();
        AffineTransform gtw = (AffineTransform) gg2D.getGridToCRS2D();

        double maskSizeX = Math.abs(gtw.getScaleX());
        double maskSizeY = Math.abs(gtw.getScaleY());

        if (!SSUtils.compareDouble(pixelSizeX, maskSizeX)
                || !SSUtils.compareDouble(pixelSizeY, maskSizeY)) {
            LOGGER.log(Level.WARNING, "Resampling mask coverage...");
            maskCoverage = resampleNearest(maskCoverage, pixelSizeX, pixelSizeY);
        }
        gridExtent = new ReferencedEnvelope(maskCoverage.getEnvelope());

        // same extent, columns, rows
        ReferencedEnvelope clipExt = new ReferencedEnvelope(maskCoverage.getEnvelope2D(), inputCrs);
        RasterClipOperation cropOp = new RasterClipOperation();
        inputCoverage = cropOp.execute(inputCoverage, clipExt);

        RasterPixelType pixelType = RasterHelper.getTransferType(inputCoverage);
        PlanarImage maskImage = (PlanarImage) maskCoverage.getRenderedImage();
        PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        DiskMemImage outputImage = this.createDiskMemImage(gridExtent, pixelType,
                maskImage.getTileWidth(), maskImage.getTileHeight());

        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());
        RectIter maskIter = RectIterFactory.create(maskImage, maskImage.getBounds());
        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        maskIter.startLines();
        inputIter.startLines();
        writerIter.startLines();
        while (!maskIter.finishedLines() && !inputIter.finishedLines()
                && !writerIter.finishedLines()) {
            maskIter.startPixels();
            inputIter.startPixels();
            writerIter.startPixels();
            while (!maskIter.finishedPixels() && !inputIter.finishedPixels()
                    && !writerIter.finishedPixels()) {
                double maskVal = maskIter.getSampleDouble(0);
                double inputVal = inputIter.getSampleDouble(0);

                if (SSUtils.compareDouble(maskNoData, maskVal)
                        || SSUtils.compareDouble(noData, inputVal)) {
                    writerIter.setSample(0, noData);
                } else {
                    writerIter.setSample(0, inputVal);
                    updateStatistics(inputVal);
                }

                maskIter.nextPixel();
                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            maskIter.nextLine();
            inputIter.nextLine();
            writerIter.nextLine();
        }

        return createGridCoverage(inputCoverage.getName(), outputImage);
    }

    private GridCoverage2D resampleNearest(GridCoverage2D coverage, double cellSizeX,
            double cellSizeY) {
        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        ReferencedEnvelope extent = new ReferencedEnvelope(coverage.getEnvelope());

        Dimension dim = RasterHelper.getDimension(extent, cellSizeX, cellSizeY);
        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, dim.width, dim.height);
        GridGeometry2D gg2D = new GridGeometry2D(gridRange, extent);

        // backgroundValues
        PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
        double[] backgroundValues = new double[inputImage.getSampleModel().getNumBands()];
        for (int index = 0; index < backgroundValues.length; index++) {
            backgroundValues[index] = noData;
        }

        return (GridCoverage2D) Operations.DEFAULT.resample(coverage, crs, gg2D, interpolation,
                backgroundValues);
    }
}
