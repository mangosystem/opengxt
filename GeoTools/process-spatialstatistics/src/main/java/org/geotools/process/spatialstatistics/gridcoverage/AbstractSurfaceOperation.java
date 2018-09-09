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
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
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

    protected GridCoverage2D grid2D;

    protected double srcNoData = -Float.MAX_VALUE;

    protected double _8DX = CellSizeX * 8;

    protected double _8DY = CellSizeY * 8;

    protected double[][] getSubMatrix(GridCoverage2D gc, GridCoordinates2D pos, int width,
            int height) {
        return getSubMatrix(gc, pos, width, height, 1.0);
    }

    protected double[][] getSubMatrix(GridCoverage2D gc, GridCoordinates2D pos, int width,
            int height, double zFactor) {
        final int posX = width / 2;
        final int posY = height / 2;

        // upper-left corner
        final GridCoordinates2D ulPos = new GridCoordinates2D(pos.x - posX, pos.y - posY);
        final Rectangle rect = new Rectangle(ulPos.x, ulPos.y, width, height);

        final RenderedImage image = gc.getRenderedImage();
        final Raster subsetRs = image.getData(rect);

        boolean hasNAN = false;
        double[][] mx = new double[width][height];
        for (int dy = ulPos.y, drow = 0; drow < subsetRs.getHeight(); dy++, drow++) {
            for (int dx = ulPos.x, dcol = 0; dcol < subsetRs.getWidth(); dx++, dcol++) {
                if (dx < 0 || dy < 0 || dx >= image.getWidth() || dy >= image.getHeight()) {
                    mx[dcol][drow] = Double.NaN;
                    hasNAN = true;
                } else {
                    final double ret = subsetRs.getSampleDouble(dx, dy, 0);
                    if (SSUtils.compareDouble(ret, this.srcNoData)) {
                        mx[dcol][drow] = Double.NaN;
                        hasNAN = true;
                    } else {
                        mx[dcol][drow] = ret * zFactor;
                    }
                }
            }
        }

        if (Double.isNaN(mx[1][1])) {
            return mx;
        }

        // http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#/How_Slope_works/009z000000vz000000/
        // If any neighborhood cells are NoData, they are assigned the value of the center cell;
        // then the slope is computed.they are assigned the value of the center cell; then the slope
        // is computed.
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