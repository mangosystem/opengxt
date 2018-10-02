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
 * TRI - Terrain Ruggedness Index is as described in Wilson et al (2007).<br>
 * This is based on the method of Valentine et al. (2004). <br>
 * Terrain Ruggedness Index is average difference in height
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterTRIOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterTRIOperation.class);

    public RasterTRIOperation() {

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

                visitTRI(writer, pos);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("TRI", outputImage);
    }

    private void visitTRI(WritableRectIter writer, GridCoordinates2D pos) {
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

        // Terrain Ruggedness Index is average difference in height
        final double tri = (Math.abs(mx[0][0] - mx[1][1]) + Math.abs(mx[1][0] - mx[1][1])
                + Math.abs(mx[2][0] - mx[1][1]) + Math.abs(mx[0][1] - mx[1][1])
                + Math.abs(mx[2][1] - mx[1][1]) + Math.abs(mx[0][2] - mx[1][1])
                + Math.abs(mx[1][2] - mx[1][1]) + Math.abs(mx[2][2] - mx[1][1])) / 8.0;

        writer.setSample(0, tri);
        updateStatistics(tri);
    }
}