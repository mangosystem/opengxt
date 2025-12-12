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
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.util.logging.Logging;

/**
 * Identifies the slope (gradient, or rate of maximum change in z-value) from each cell of a raster surface.
 * <p>
 * Burrough, P. A., and McDonell, R. A., 1998. Principles of Geographical Information Systems (Oxford University Press, New York), 190 pp.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSlopeOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterSlopeOperation.class);

    public RasterSlopeOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputGc, SlopeType slopeType) {
        return execute(inputGc, slopeType, 1.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, SlopeType slopeType, double zFactor) {
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

                visitSlope(writer, pos, slopeType, zFactor);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("Slope", outputImage);
    }

    private void visitSlope(WritableRectIter writer, GridCoordinates2D pos, SlopeType slopeType,
            double zFactor) {
        // http://webhelp.esri.com/arcgisdesktop/9.3/index.cfm?TopicName=How%20Slope%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.
        // [dz/dx] = ((c + 2f + i) - (a + 2d + g) / (8 * x_cell_size)
        // [dz/dy] = ((g + 2h + i) - (a + 2b + c)) / (8 * y_cell_size)
        // slope_degrees = ATAN ( ( [dz/dx]2 + [dz/dy]2 ) ) * 57.29578

        // For degrees, the range of slope values is 0 to 90.
        // For percent rise, the range is 0 to essentially infinity.
        // A flat surface is 0 percent, a 45 degree surface is 100 percent
        // If the center cell in the immediate neighborhood (3 x 3 window) is NoData,
        // the output is NoData.
        // If any neighborhood cells are NoData, they are assigned the value of the center cell;
        // then the slope is computed.

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3, zFactor);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, noData);
            return;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DY);

        double rise_run = (dZdX * dZdX) + (dZdY * dZdY);
        if (Double.isNaN(rise_run) || Double.isInfinite(rise_run)) {
            writer.setSample(0, noData);
            return;
        }

        double slope = Math.atan(Math.sqrt(rise_run));

        if (slopeType == SlopeType.Degree) {
            slope = Math.toDegrees(slope);
        } else {
            slope = Math.tan(slope) * 100;
        }

        if (slope < 0 || slope > 100) {
            writer.setSample(0, noData);
            return;
        }

        writer.setSample(0, slope);
        updateStatistics(slope);
    }
}
