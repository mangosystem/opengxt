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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Derives Normalized Difference Vegetation Index (NDVI) from two rasters.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterNDVIOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterNDVIOperation.class);

    private double nirNoData = Float.MIN_VALUE;

    private double redNoData = Float.MIN_VALUE;

    public GridCoverage2D execute(GridCoverage2D nirCoverage, Integer nirIndex,
            GridCoverage2D redCoverage, Integer redIndex) {

        // Extent, CellSize, CRS 확인 후 동일하게 리샘플링 후 계산
        nirNoData = RasterHelper.getNoDataValue(nirCoverage);
        redNoData = RasterHelper.getNoDataValue(redCoverage);
        this.NoData = nirNoData; // default nodata

        double nirCellSize = RasterHelper.getCellSize(nirCoverage);
        double redCellSize = RasterHelper.getCellSize(redCoverage);

        ReferencedEnvelope nirExtent = new ReferencedEnvelope(nirCoverage.getEnvelope());
        ReferencedEnvelope redExtent = new ReferencedEnvelope(redCoverage.getEnvelope());

        CoordinateReferenceSystem nirCRS = nirCoverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem redCRS = redCoverage.getCoordinateReferenceSystem();

        // create image
        if (nirExtent.equals(redExtent) && nirCellSize == redCellSize) {
            return executeNDVI(nirCoverage, nirIndex, redCoverage, redIndex);
        } else if (nirExtent.equals(redExtent) && nirCellSize != redCellSize) {
            // resample
            RasterResampleOperation resample = new RasterResampleOperation();
            GridCoverage2D resampled = resample.execute(redCoverage, nirCellSize,
                    ResampleType.NEAREST);

            return executeNDVI(nirCoverage, nirIndex, resampled, redIndex);
        } else {
            boolean equalCRS = CRS.equalsIgnoreMetadata(nirCRS, redCRS);
            if (equalCRS) {
                ReferencedEnvelope extent = nirExtent.intersection(redExtent); // intersection

                RasterCropOperation crop = new RasterCropOperation();

                if (!extent.equals(nirExtent)) {
                    nirCoverage = crop.execute(nirCoverage, extent);
                }

                if (!extent.equals(redExtent)) {
                    redCoverage = crop.execute(redCoverage, extent);
                }

                return execute(nirCoverage, nirIndex, redCoverage, redIndex);
            } else {
                // reproject
                RasterReprojectOperation reproject = new RasterReprojectOperation();
                redCoverage = reproject.execute(redCoverage, nirCRS, ResampleType.NEAREST,
                        nirCellSize);

                return execute(nirCoverage, nirIndex, redCoverage, redIndex);
            }
        }
    }

    private GridCoverage2D executeNDVI(GridCoverage2D nirCoverage, Integer nirIndex,
            GridCoverage2D redCoverage, Integer redIndex) {
        DiskMemImage outputImage = this.createDiskMemImage(nirCoverage, RasterPixelType.DOUBLE);

        PlanarImage nirImage = (PlanarImage) nirCoverage.getRenderedImage();
        PlanarImage redImage = (PlanarImage) redCoverage.getRenderedImage();

        RectIter nirIter = RectIterFactory.create(nirImage, nirImage.getBounds());
        RectIter redIter = RectIterFactory.create(redImage, redImage.getBounds());
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        nirIter.startLines();
        redIter.startLines();
        writerIter.startLines();
        while (!nirIter.finishedLines() && !redIter.finishedLines() && !writerIter.finishedLines()) {
            nirIter.startPixels();
            redIter.startPixels();
            writerIter.startPixels();
            while (!nirIter.finishedPixels() && !redIter.finishedPixels()
                    && !writerIter.finishedPixels()) {
                double nir = nirIter.getSampleDouble(nirIndex);
                double red = redIter.getSampleDouble(redIndex);

                if (SSUtils.compareDouble(nir, nirNoData) || SSUtils.compareDouble(nir, redNoData)) {
                    writerIter.setSample(0, this.NoData);
                } else {
                    // NDVI = ((IR - R)/(IR + R)) = -1.0 ~ 1.0
                    double ndviValue = (nir - red) / (nir + red);
                    writerIter.setSample(0, ndviValue);
                    // updateStatistics(ndviValue); // skip
                }
                nirIter.nextPixel();
                redIter.nextPixel();
                writerIter.nextPixel();
            }
            nirIter.nextLine();
            redIter.nextLine();
            writerIter.nextLine();
        }

        this.MinValue = -1.0;
        this.MaxValue = 1.0;

        return createGridCoverage("NDVI", outputImage);
    }
}
