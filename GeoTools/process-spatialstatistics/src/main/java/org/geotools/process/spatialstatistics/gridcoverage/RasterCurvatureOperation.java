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
 * Calculates the curvature of a raster surface.
 * <p>
 * Moore, I. D., R. B. Grayson, and A. R. Landson. 1991. Digital Terrain Modelling: A Review of Hydrological, Geomorphological, and Biological
 * Applications. Hydrological Processes 5: 3–30.
 * <p>
 * Zeverbergen, L. W., and C. R. Thorne. 1987. Quantitative Analysis of Land Surface Topography. Earth Surface Processes and Landforms 12: 47–56.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCurvatureOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCurvatureOperation.class);

    private double xL2;

    private double x2L;

    private double yL2;

    private double y2L;

    public GridCoverage2D execute(GridCoverage2D inputGc) {
        return execute(inputGc, 1.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, double zFactor) {
        DiskMemImage outputImage = this.createDiskMemImage(inputGc, RasterPixelType.FLOAT);

        grid2D = inputGc;
        srcNoData = RasterHelper.getNoDataValue(inputGc);
        NoData = -9999;

        xL2 = CellSizeX * CellSizeX;
        x2L = 2.0 * CellSizeX;

        yL2 = CellSizeY * CellSizeY;
        y2L = 2.0 * CellSizeY;

        final java.awt.Rectangle bounds = outputImage.getBounds();
        WritableRectIter writer = RectIterFactory.createWritable(outputImage, bounds);

        GridCoordinates2D pos = new GridCoordinates2D();

        int y = 0;
        writer.startLines();
        while (!writer.finishedLines()) {

            int x = 0;
            writer.startPixels();
            while (!writer.finishedPixels()) {
                pos.setLocation(x, y);

                visitCurvature(writer, pos, zFactor);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("Curvature", outputImage);
    }

    private void visitCurvature(WritableRectIter writer, GridCoordinates2D pos, double zFactor) {
        // http://resources.arcgis.com/en/help/main/10.1/#/How_Curvature_works/009z000000vs000000/
        // Zeverbergen, L. W., and C. R. Thorne. 1987. Quantitative Analysis of Land Surface
        // Topography.
        // Earth Surface Processes and Landforms 12: 47–56.

        double[][] mx = getSubMatrix(grid2D, pos, 3, 3, zFactor);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, NoData);
            return;
        }

        // Z = Ax²y² + Bx²y + Cxy² + Dx² + Ey² + Fxy + Gx + Hy + I

        // A = [(Z1 + Z3 + Z7 + Z9) / 4 - (Z2 + Z4 + Z6 + Z8) / 2 + Z5] / L4
        // double A = ((Z1 + Z3 + Z7 + Z9) / 4 - (Z2 + Z4 + Z6 + Z8) / 2 + Z5) / L4 ;

        // B = [(Z1 + Z3 - Z7 - Z9) /4 - (Z2 - Z8) /2] / L3
        // double B = ((Z1 + Z3 - Z7 - Z9) /4 - (Z2 - Z8) /2) / L3;

        // C = [(-Z1 + Z3 - Z7 + Z9) /4 + (Z4 - Z6)] /2] / L3
        // double C = ((-Z1 + Z3 - Z7 + Z9) /4 + (Z4 - Z6)] /2) / L3;

        // +-------+ +----------+
        // | 0 1 2 | | Z1 Z2 Z3 |
        // | 3 4 5 |>| Z4 Z5 Z6 |
        // | 6 7 8 | | Z7 Z8 Z9 |
        // +-------+ +----------+
        // D = [(Z4 + Z6) /2 - Z5] / L2
        double D = ((mx[0][1] + mx[2][1]) / 2.0 - mx[1][1]) / xL2;

        // E = [(Z2 + Z8) /2 - Z5] / L2
        double E = ((mx[1][0] + mx[1][2]) / 2.0 - mx[1][1]) / yL2;

        // F = (-Z1 + Z3 + Z7 - Z9) / 4L2
        // double F = (mx[2][0] - mx[0][0] + mx[0][2] - mx[2][2]) / x4L2;

        // G = (-Z4 + Z6) / 2L
        double G = (mx[2][1] - mx[0][1]) / x2L;

        // H = (Z2 - Z8) / 2L
        double H = (mx[1][0] - mx[1][2]) / y2L;

        // I = Z5
        // double I = Z5;

        // The output of the Curvature tool is the second derivative of the surface—for example,
        // the slope of the slope—such that: Curvature = -2(D + E) * 100

        final double k2 = G * G + H * H;

        double curvature = 0;
        if (k2 != 0) {
            curvature = -2.0 * (E + D);

            // optional Horizontal curvature & Vertical curvature
            // double k1 = F * G * H;
            // double vCurv = -2.0 * (D * G * G + E * H * H + k1) / k2;
            // double hCurv = -2.0 * (D * H * H + E * G * G - k1) / k2;
        }

        // Units of the curvature output raster, as well as the units for the optional output
        // profile curve raster and output plan curve raster, are one hundredth (1/100) of a z-unit.

        curvature = curvature * (100.0 * zFactor);

        writer.setSample(0, curvature);
        updateStatistics(curvature);
    }
}