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

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.iterator.RectIterFactory;
import org.eclipse.imagen.iterator.WritableRectIter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * Calculates the volume change between two surfaces.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFillOperation2 extends AbstractRasterCutFillOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillOperation2.class);

    public RasterCutFillOperation2() {

    }

    public SimpleFeatureCollection execute(GridCoverage2D beforeDEM, GridCoverage2D afterDEM,
            Geometry cropShape) throws ProcessException, IOException {
        if (cropShape == null || cropShape.isEmpty()) {
            throw new ProcessException("cropShape is null or empty!");
        }

        if (!validateProperties(beforeDEM, afterDEM)) {
            throw new ProcessException(
                    "beforeDEM and afterDEM must have the same coordinate system and cell size!");
        }

        // check cell size
        RasterClipOperation cropOp = new RasterClipOperation();
        GridCoverage2D beforeGc = cropOp.execute(beforeDEM, cropShape);
        GridCoverage2D afterGc = cropOp.execute(afterDEM, cropShape);

        final double beforeNoData = RasterHelper.getNoDataValue(beforeGc);
        final double afaterNoData = RasterHelper.getNoDataValue(afterGc);

        GridGeometry2D beforeGG2D = beforeDEM.getGridGeometry();
        AffineTransform beforeTrans = (AffineTransform) beforeGG2D.getGridToCRS2D();

        double beforeX = Math.abs(beforeTrans.getScaleX());
        double beforeY = Math.abs(beforeTrans.getScaleY());

        final int outputNoData = -9999;
        final double cellArea = beforeX * beforeY;
        CutFillResult result = new CutFillResult(0d);

        DiskMemImage outputImage = createDiskMemImage(beforeGc, RasterPixelType.SHORT);
        PlanarImage inputImage = (PlanarImage) beforeGc.getRenderedImage();
        PlanarImage afterImage = (PlanarImage) afterGc.getRenderedImage();

        RectIter beforeIter = RectIterFactory.create(inputImage, inputImage.getBounds());
        RectIter afterIter = RectIterFactory.create(afterImage, afterImage.getBounds());
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        beforeIter.startLines();
        afterIter.startLines();
        writerIter.startLines();
        while (!beforeIter.finishedLines() && !afterIter.finishedLines()
                && !writerIter.finishedLines()) {
            beforeIter.startPixels();
            afterIter.startPixels();
            writerIter.startPixels();

            while (!beforeIter.finishedPixels() && !afterIter.finishedPixels()
                    && !writerIter.finishedPixels()) {
                final double beforeVal = beforeIter.getSampleDouble(0);
                final double afterVal = afterIter.getSampleDouble(0);

                // Cut = 1, Fill = -1, Unchanged = 0
                int flag = outputNoData;

                if (!SSUtils.compareDouble(beforeNoData, beforeVal)
                        && !SSUtils.compareDouble(afaterNoData, afterVal)) {
                    double diffVal = beforeVal - afterVal;
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

                beforeIter.nextPixel();
                afterIter.nextPixel();
                writerIter.nextPixel();
            }

            beforeIter.nextLine();
            afterIter.nextLine();
            writerIter.nextLine();
        }

        cutFillRaster = createGridCoverage("CutFill", outputImage, 0, outputNoData, -1, 1, gridExtent);

        // finally build features
        return buildFeatures(cutFillRaster, result);
    }

    private boolean validateProperties(GridCoverage2D beforeDEM, GridCoverage2D afterDEM) {
        CoordinateReferenceSystem beforeCRS = beforeDEM.getCoordinateReferenceSystem();
        CoordinateReferenceSystem afterCRS = afterDEM.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(beforeCRS, afterCRS)) {
            return false;
        }

        GridGeometry2D beforeGG2D = beforeDEM.getGridGeometry();
        AffineTransform beforeTrans = (AffineTransform) beforeGG2D.getGridToCRS2D();

        double beforeX = Math.abs(beforeTrans.getScaleX());
        double beforeY = Math.abs(beforeTrans.getScaleY());

        GridGeometry2D afterGG2D = afterDEM.getGridGeometry();
        AffineTransform afterTrans = (AffineTransform) afterGG2D.getGridToCRS2D();

        double afterX = Math.abs(afterTrans.getScaleX());
        double afterY = Math.abs(afterTrans.getScaleY());

        if (!SSUtils.compareDouble(beforeX, afterX) || !SSUtils.compareDouble(beforeY, afterY)) {
            return false;
        }

        return true;
    }
}
