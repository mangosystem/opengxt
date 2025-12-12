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
import org.locationtech.jts.geom.Coordinate;

/**
 * Creates a raster of flow direction from each cell to its downslope neighbor, or neighbors, using D8 methods.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFlowDirectionOperation extends AbstractSurfaceOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterFlowDirectionOperation.class);

    public RasterFlowDirectionOperation() {

    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage) {
        this.initSurface(inputCoverage);

        DiskMemImage outputImage = this.createDiskMemImage(inputCoverage, RasterPixelType.INTEGER);
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

        return createGridCoverage("FlowDirection", outputImage);
    }

    private void visitAspect(WritableRectIter writer, GridCoordinates2D pos) {
        // https://pro.arcgis.com/en/pro-app/tool-reference/spatial-analyst/how-flow-direction-works.htm
        // +-------+ +-----------+
        // | 0 1 2 | | 32 64 128 |
        // | 3 4 5 |>| 16 x 1 |
        // | 6 7 8 | | 8 4 2 |
        // +-------+ +-----------+

        double[][] mx = getSubMatrix(pos, 3, 3);
        if (Double.isNaN(mx[1][1]) || SSUtils.compareDouble(srcNoData, mx[1][1])) {
            writer.setSample(0, noData);
            return;
        }

        Coordinate center_pos = new Coordinate(1, 1);
        Coordinate max_drop_pos = null;

        int flowDir = -1;
        double max_drop = 0;
        for (int row = 0; row < 3; row++) {
            int dY = (int) (center_pos.y - row);
            for (int col = 0; col < 3; col++) {
                if (col == 1 && row == 1) {
                    continue;
                }

                // The distance is calculated between cell centers.
                // Therefore, if the cell size is 1, the distance between two orthogonal cells is 1,
                // and the distance between two diagonal cells is 1.414 (the square root of 2)
                int dX = (int) (center_pos.x - col);

                // maximum_drop = change_in_z-value / distance * 100
                double drop = (mx[1][1] - mx[col][row]) / (Math.hypot(dX, dY) * 100);
                if (max_drop_pos == null || drop > max_drop) {
                    max_drop_pos = new Coordinate(col, row);
                    max_drop = drop;
                }
            }
        }

        flowDir = getFlowDirection(center_pos, max_drop_pos);

        writer.setSample(0, flowDir);
        updateStatistics(flowDir);
    }

    private int getFlowDirection(Coordinate from, Coordinate to) {
        // +-------+ +-----------+
        // | 0 1 2 | | 32 64 128 |
        // | 3 4 5 |>| 16 x 1 |
        // | 6 7 8 | | 8 4 2 |
        // +-------+ +-----------+

        int directionPow = -1;
        if (from.y == to.y) {
            if (from.x < to.x) {
                directionPow = 0; // 1
            } else if (from.x == to.x) {
                directionPow = -1; // c
            } else {
                directionPow = 4; // 16
            }
        } else if (from.y < to.y) {
            if (from.x < to.x) {
                directionPow = 1; // 2
            } else if (from.x == to.x) {
                directionPow = 2; // 4
            } else {
                directionPow = 3; // 8
            }
        } else {
            if (from.x < to.x) {
                directionPow = 7; // 128
            } else if (from.x == to.x) {
                directionPow = 6; // 64
            } else {
                directionPow = 5; // 32
            }
        }

        if (directionPow <= -1) {
            return 0;
        }

        return (int) Math.pow(2, directionPow);
    }
}
