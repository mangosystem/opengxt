/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.DataStatistics;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Geometry;

/**
 * Calculates the volume change between two surfaces.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFillOperation extends AbstractRasterCutFillOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillOperation.class);

    public RasterCutFillOperation() {

    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Geometry cropShape)
            throws ProcessException, IOException {
        return execute(inputDEM, cropShape, -9999.0);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Geometry cropShape,
            double baseHeight) throws ProcessException, IOException {
        RasterCropOperation cropOp = new RasterCropOperation();
        GridCoverage2D cropedCoverage = cropOp.execute(inputDEM, cropShape);

        SimpleFeatureCollection result = null;
        if (cropedCoverage != null) {
            if (baseHeight == -9999.0) {
                DataStatistics statOp = new DataStatistics();
                StatisticsVisitorResult ret = statOp.getStatistics(cropedCoverage, 0);
                baseHeight = ret.getMean();
            }

            result = execute(cropedCoverage, baseHeight);
        }

        return result;
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, double baseHeight)
            throws ProcessException, IOException {
        NoData = RasterHelper.getNoDataValue(inputDEM);

        GridGeometry2D gridGeometry2D = inputDEM.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        CellSizeX = Math.abs(gridToWorld.getScaleX());
        CellSizeY = Math.abs(gridToWorld.getScaleY());
        Extent = new ReferencedEnvelope(inputDEM.getEnvelope());

        final int outputNoData = -9999;
        final double cellArea = CellSizeX * CellSizeY;
        CutFillResult result = new CutFillResult(baseHeight);

        PlanarImage inputImage = (PlanarImage) inputDEM.getRenderedImage();

        DiskMemImage outputImage = createDiskMemImage(inputDEM, RasterPixelType.SHORT);
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        readIter.startLines();
        writerIter.startLines();
        while (!readIter.finishedLines() && !writerIter.finishedLines()) {
            readIter.startPixels();
            writerIter.startPixels();

            while (!readIter.finishedPixels() && !writerIter.finishedPixels()) {
                final double gridVal = readIter.getSampleDouble(0);

                // Cut = 1, Fill = -1, Unchanged = 0
                int flag = outputNoData;

                if (!SSUtils.compareDouble(NoData, gridVal)) {
                    double diffVal = gridVal - baseHeight;
                    double volume = Math.abs(cellArea * diffVal);

                    if (diffVal > 0) {
                        result.cutArea += cellArea;
                        result.cutVolume += volume;
                        result.cutCount += 1;
                        flag = 1;
                    } else if (diffVal < 0) {
                        result.fillArea += cellArea;
                        result.fillVolume += volume;
                        result.fillCount += 1;
                        flag = -1;
                    } else {
                        result.unChangedArea += cellArea;
                        result.unChangedCount += 1;
                        flag = 0;
                    }
                }

                writerIter.setSample(0, flag);

                readIter.nextPixel();
                writerIter.nextPixel();
            }

            readIter.nextLine();
            writerIter.nextLine();
        }

        cutFillRaster = createGridCoverage("CutFill", outputImage, 0, outputNoData, -1, 1, Extent);

        // finally build features
        return buildFeatures(cutFillRaster, result);
    }
}