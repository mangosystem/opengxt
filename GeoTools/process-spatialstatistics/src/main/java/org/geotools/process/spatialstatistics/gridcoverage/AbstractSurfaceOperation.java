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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.util.logging.Logger;

import org.eclipse.imagen.PlanarImage;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.util.logging.Logging;

/**
 * Abstract Surface Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractSurfaceOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractSurfaceOperation.class);

    protected double srcNoData = -Float.MAX_VALUE;

    protected double _8DX = pixelSizeX * 8;

    protected double _8DY = pixelSizeY * 8;

    protected PlanarImage image;

    protected java.awt.Rectangle bounds;

    private int maxCol;

    private int maxRow;

    protected void initSurface(GridCoverage2D gc) {
        GridGeometry2D gridGeometry2D = gc.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        pixelSizeX = Math.abs(gridToWorld.getScaleX());
        pixelSizeY = Math.abs(gridToWorld.getScaleY());

        srcNoData = RasterHelper.getNoDataValue(gc);
        noData = -9999;

        _8DX = pixelSizeX * 8;
        _8DY = pixelSizeY * 8;

        image = (PlanarImage) gc.getRenderedImage();
        bounds = image.getBounds();

        maxCol = bounds.x + image.getWidth();
        maxRow = bounds.y + image.getHeight();
    }

    protected double[][] getSubMatrix(GridCoordinates2D pos, int width, int height) {
        return getSubMatrix(pos, width, height, 1.0);
    }

    protected double[][] getSubMatrix(GridCoordinates2D pos, int width, int height, double zFactor) {
        int posX = width / 2;
        int posY = height / 2;

        // upper-left corner
        GridCoordinates2D ulPos = new GridCoordinates2D(pos.x - posX, pos.y - posY);
        Rectangle rect = new Rectangle(ulPos.x, ulPos.y, width, height);

        Raster subsetRs = image.getData(rect);

        boolean hasNAN = false;
        double[][] mx = new double[width][height];
        for (int dy = ulPos.y, row = 0; row < height; dy++, row++) {
            for (int dx = ulPos.x, col = 0; col < width; dx++, col++) {
                if (dx < bounds.x || dy < bounds.y || dx >= maxCol || dy >= maxRow) {
                    mx[col][row] = Double.NaN;
                    hasNAN = true;
                } else {
                    double ret = subsetRs.getSampleDouble(dx, dy, 0);
                    if (SSUtils.compareDouble(ret, this.srcNoData)) {
                        mx[col][row] = Double.NaN;
                        hasNAN = true;
                    } else {
                        mx[col][row] = ret * zFactor;
                    }
                }
            }
        }

        if (Double.isNaN(mx[1][1])) {
            return mx;
        }

        // http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#/How_Slope_works/009z000000vz000000/
        // If any neighborhood cells are NoData, they are assigned the value of the center cell;
        // then the slope is computed.
        if (hasNAN) {
            for (int drow = 0; drow < height; drow++) {
                for (int dcol = 0; dcol < width; dcol++) {
                    if (Double.isNaN(mx[dcol][drow])) {
                        mx[dcol][drow] = mx[1][1];
                    }
                }
            }
        }

        return mx;
    }
}