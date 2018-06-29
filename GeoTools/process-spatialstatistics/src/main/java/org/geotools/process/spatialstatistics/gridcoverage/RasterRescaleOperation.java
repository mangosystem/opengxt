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

import java.awt.geom.AffineTransform;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Resizes a raster by the specified x and y scale factors.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRescaleOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterRescaleOperation.class);

    /**
     * Resizes a raster by the specified x and y scale factors.
     * 
     * @param inputCoverage The input raster.
     * @param x_scale The factor in which to scale the cell size in the x direction. The factor must be greater than zero.
     * @param y_scale The factor in which to scale the cell size in the y direction. The factor must be greater than zero.
     * @return GridCoverage2D
     */
    public GridCoverage2D execute(GridCoverage2D inputCoverage, double x_scale, double y_scale) {
        if (x_scale <= 0) {
            throw new InvalidParameterValueException("x_scale must be greater than zero",
                    "x_scale", x_scale);
        }

        if (y_scale <= 0) {
            throw new InvalidParameterValueException("y_scale must be greater than zero",
                    "y_scale", y_scale);
        }

        this.initilizeVariables(inputCoverage);

        ReferencedEnvelope extent = new ReferencedEnvelope(inputCoverage.getEnvelope());
        GridGeometry2D gridGeometry2D = inputCoverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();
        final double cellSizeX = Math.abs(gridToWorld.getScaleX()) * x_scale;
        final double cellSizeY = Math.abs(gridToWorld.getScaleY()) * y_scale;
        CellSize = Math.max(cellSizeX, cellSizeY);

        // 1. The output size is multiplied by the scale factor for both the x and y directions. The
        // number of columns and rows stays the same in this process, but the cell size is
        // multiplied by the scale factor.
        // 2. The scale factor must be positive.
        // 3. A scale factor greater than one means the image will be rescaled to a larger
        // dimension,
        // resulting in a larger extent because of a larger cell size.
        // 4. A scale factor less than one means the image will be rescaled to a smaller dimension,
        // resulting in a smaller extent because of a smaller cell size.

        // Rescale extent
        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        double maxX = extent.getMinX() + (inputImage.getWidth() * cellSizeX);
        double maxY = extent.getMinY() + (inputImage.getHeight() * cellSizeY);

        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        Extent = new ReferencedEnvelope(extent.getMinX(), maxX, extent.getMinY(), maxY, crs);

        return createGridCoverage(inputCoverage.getName(), inputImage);
    }
}
