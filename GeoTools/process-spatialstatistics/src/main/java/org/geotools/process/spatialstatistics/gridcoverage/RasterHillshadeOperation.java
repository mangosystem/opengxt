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
 * Creates a shaded relief from a surface raster by considering the illumination source angle and shadows.
 * <p>
 * Burrough, P. A. and McDonell, R. A., 1998. Principles of Geographical Information Systems (Oxford University Press, New York), 190 pp.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHillshadeOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterHillshadeOperation.class);

    private final double DEGTORAD = Math.PI / 180.0;

    private final double HALFPI = Math.PI / 2.0;

    public RasterHillshadeOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputGc) {
        return execute(inputGc, 315.0, 45.0, 1.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, double azimuth, double altitude) {
        return execute(inputGc, azimuth, altitude, 1.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputGc, double azimuth, double altitude,
            double zFactor) {
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

                visitHillShade(writer, pos, azimuth, altitude, zFactor);

                writer.nextPixel();
                x++;
            }

            writer.nextLine();
            y++;
        }

        return createGridCoverage("Aspect", outputImage);
    }

    private void visitHillShade(WritableRectIter writer, GridCoordinates2D pos,
            final double azimuth, final double altitude, final double zFactor) {
        // http://webhelp.esri.com/arcgisdesktop/9.2/index.cfm?TopicName=How%20Hillshade%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.

        // The hillshade raster has an integer value range of 0 to 255.

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3, zFactor);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, NoData);
            return;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DY);

        if (Double.isNaN(dZdX) || Double.isNaN(dZdY) || Double.isInfinite(dZdX)
                || Double.isInfinite(dZdY)) {
            writer.setSample(0, NoData);
            return;
        }

        // Computing the illumination angle
        double zenith_deg = 90 - altitude;
        double zenith_rad = zenith_deg * DEGTORAD;

        // Computing the illumination direction
        double azimuth_math = 360.0 - azimuth + 90;
        if (azimuth_math >= 360) {
            azimuth_math = azimuth_math - 360.0;
        }
        double azimuth_rad = azimuth_math * DEGTORAD;

        // Computing Slope and Aspect
        // Slope_rad = ATAN (z_factor * √ ([dz/dx]2 + [dz/dy]2))
        double slope_rad = Math.atan(zFactor * Math.sqrt((dZdX * dZdX) + (dZdY * dZdY)));

        double aspect_rad = 0.0;
        if (dZdX == 0.0) {
            if (dZdY > 0) {
                aspect_rad = HALFPI;
            } else if (dZdY < 0) {
                aspect_rad = (2.0 * Math.PI) - HALFPI;
            } else {
                aspect_rad = Math.atan2(dZdY, -dZdX);
                if (aspect_rad < 0) {
                    aspect_rad = (2.0 * Math.PI) + aspect_rad;
                }
            }
        } else {
            aspect_rad = Math.atan2(dZdY, -dZdX);
            if (aspect_rad < 0) {
                aspect_rad = (2.0 * Math.PI) + aspect_rad;
            }
        }

        // The hillshade algorithm
        // 255.0 * ((cos(Zenith_rad) * cos(Slope_rad)) + (sin(Zenith_rad) * sin(Slope_rad) *
        // cos(Azimuth_rad - Aspect_rad)))
        double hsdVal = 255.0 * ((Math.cos(zenith_rad) * Math.cos(slope_rad)) + (Math
                .sin(zenith_rad) * Math.sin(slope_rad) * Math.cos(azimuth_rad - aspect_rad)));

        // Note that if the calculation of the hillshade value is < 0, the output cell value will be
        // = 0.
        if (hsdVal < 0) {
            hsdVal = 0;
        } else if (hsdVal > 255) {
            hsdVal = 255;
        }

        writer.setSample(0, hsdVal);
        updateStatistics(hsdVal);
    }
}