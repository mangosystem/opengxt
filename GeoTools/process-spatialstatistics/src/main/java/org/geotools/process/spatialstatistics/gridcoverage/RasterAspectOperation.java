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

import org.eclipse.imagen.iterator.WritableRectIter;
import org.eclipse.imagen.iterator.RectIterFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;

/**
 * Derives aspect from a raster surface. The aspect identifies the downslope direction of the maximum rate of change in value from each cell to its
 * neighbors.
 * <p>
 * Burrough, P. A. and McDonell, R. A., 1998. Principles of Geographical Information Systems (Oxford University Press, New York), 190 pp.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterAspectOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterAspectOperation.class);

    private final double RADTODEG = 180.0 / Math.PI;

    public RasterAspectOperation() {

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

                visitAspect(writer, pos);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("Aspect", outputImage);
    }

    private void visitAspect(WritableRectIter writer, GridCoordinates2D pos) {
        // http://webhelp.esri.com/arcgisdesktop/9.3/index.cfm?TopicName=How%20Aspect%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.
        // [dz/dx] = ((c + 2f + i) - (a + 2d + g)) / 8
        // [dz/dy] = ((g + 2h + i) - (a + 2b + c)) / 8
        // aspect = 57.29578 * atan2([dz/dy], -[dz/dx])

        // Aspect is expressed in positive degrees from 0 to 359.9, measured clockwise from north.
        // Cells in the input raster that are flat—with zero slope—are assigned an aspect of -1.
        // If the center cell in the immediate neighborhood (3 x 3 window) is NoData,
        // the output is NoData.
        // If any neighborhood cells are NoData, they are first assigned the value of the center
        // cell, then the aspect is computed.

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, noData);
            return;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DY);

        double rise_run = (dZdX * dZdX) + (dZdY * dZdY);
        double slope = Math.toDegrees(Math.atan(Math.sqrt(rise_run)));
        if (Double.isNaN(slope) || Double.isInfinite(slope) || slope == 0) {
            writer.setSample(0, -1);
            updateStatistics(-1);
            return;
        }

        // aspect
        dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2])) / (8.0);
        dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0])) / (8.0);

        // double aspect = Math.toDegrees(Math.atan2(H, -G));
        double aspect = Math.atan2(dZdY, -dZdX) * RADTODEG;

        if (aspect < 0) {
            aspect = 90.0 - aspect;
        } else if (aspect > 90.0) {
            aspect = 360.0 - aspect + 90.0;
        } else {
            aspect = 90.0 - aspect;
        }

        if (aspect < 0 || aspect > 360) {
            aspect = -1.0;
        }

        writer.setSample(0, aspect);
        updateStatistics(aspect);
    }
}
