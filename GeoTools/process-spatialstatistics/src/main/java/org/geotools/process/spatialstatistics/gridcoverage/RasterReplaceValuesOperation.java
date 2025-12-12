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

import java.awt.image.WritableRaster;
import java.util.logging.Logger;

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.iterator.RectIterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Replace raster values within polygon with specific value.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReplaceValuesOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterReplaceValuesOperation.class);

    public RasterReplaceValuesOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Geometry region,
            double replaceValue) {
        if (region == null || region.isEmpty()) {
            return inputCoverage;
        }

        Envelope regionEnv = region.getEnvelopeInternal();
        if (regionEnv.getWidth() == 0d || regionEnv.getHeight() == 0d) {
            return inputCoverage;
        }

        // create template raster
        RasterPixelType pixelType = RasterHelper.getTransferType(inputCoverage);
        DiskMemImage outputImage = this.createDiskMemImage(inputCoverage, pixelType);
        outputImage.setData(inputCoverage.getRenderedImage().getData());

        WritableRaster outputRaster = (WritableRaster) outputImage.getData();

        this.noData = RasterHelper.getNoDataValue(inputCoverage);
        GridTransformer inputTrans = new GridTransformer(inputCoverage);

        // convert region geometry to raster
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        Geometry aoiGeom = super.transformGeometry(region, crs);

        GeometryToRasterOperation geometryToRaster = new GeometryToRasterOperation();
        geometryToRaster.setExtentAndCellSize(
                new ReferencedEnvelope(aoiGeom.getEnvelopeInternal(), crs), pixelSizeX, pixelSizeY);

        GridCoverage2D aoiGc = geometryToRaster.execute(aoiGeom, crs, replaceValue, pixelType);
        double aoiNoData = RasterHelper.getNoDataValue(aoiGc);
        GridTransformer aoiTrans = new GridTransformer(aoiGc);

        // replace values
        PlanarImage inputImage = (PlanarImage) aoiGc.getRenderedImage();
        RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        int valid = 0;
        int row = 0;
        readIter.startLines();
        while (!readIter.finishedLines()) {
            int column = 0;
            readIter.startPixels();

            while (!readIter.finishedPixels()) {
                final double gridVal = readIter.getSampleDouble(0);

                if (SSUtils.compareDouble(gridVal, noData)
                        || SSUtils.compareDouble(gridVal, aoiNoData)) {
                    column++;
                    readIter.nextPixel();
                    continue;
                }

                Coordinate worldPos = aoiTrans.gridToWorldCoordinate(column, row);
                GridCoordinates2D gridPos = inputTrans.worldToGrid(worldPos.x, worldPos.y);
                if (inputTrans.contains(gridPos.x, gridPos.y)) {
                    outputRaster.setSample(gridPos.x, gridPos.y, 0, gridVal);
                }

                // TODO
                updateStatistics(gridVal);

                valid++;
                column++;
                readIter.nextPixel();
            }

            row++;
            readIter.nextLine();
        }

        if (valid == 0) {
            return inputCoverage;
        }

        outputImage.setData(outputRaster);

        return createGridCoverage(inputCoverage.getName(), outputImage);
    }
}
