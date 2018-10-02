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

import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

/**
 * TPI - Topographic Position Index follows the description in Wilson et al (2007), following Weiss (2001). <br>
 * Topographic Position Index is the difference between the central cell and the mean of the surrounding cells
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterTPIOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterTPIOperation.class);

    public RasterTPIOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputGc) {
        this.initSurface(inputGc);

        DiskMemImage outputImage = this.createDiskMemImage(inputGc, RasterPixelType.FLOAT);
        WritableRectIter writer = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        GridCoordinates2D pos = new GridCoordinates2D();

        int y = bounds.y;
        writer.startLines();
        while (!writer.finishedLines()) {

            int x = bounds.x;
            writer.startPixels();
            while (!writer.finishedPixels()) {
                pos.setLocation(x, y);

                visitTPI(writer, pos);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("TPI", outputImage);
    }

    private void visitTPI(WritableRectIter writer, GridCoordinates2D pos) {
        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, NoData);
            return;
        }

        // Topographic Position Index is the difference between
        // The central cell and the mean of the surrounding cells
        double tpi = mx[1][1]
                - ((mx[0][0] + mx[1][0] + mx[2][0] + mx[0][1] + mx[2][1] + mx[0][2] + mx[1][2] + mx[2][2]) / 8.0);

        writer.setSample(0, tpi);
        updateStatistics(tpi);
    }
}