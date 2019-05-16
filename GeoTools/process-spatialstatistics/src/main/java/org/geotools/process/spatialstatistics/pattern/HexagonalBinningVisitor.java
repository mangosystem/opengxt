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
package org.geotools.process.spatialstatistics.pattern;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.util.CoordinateTranslateFilter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Hexagonal Binning Visitor.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 * @reference http://www.redblobgames.com/grids/hexagons/
 * 
 */
public class HexagonalBinningVisitor extends AbstractBinningVisitor {
    protected static final Logger LOGGER = Logging.getLogger(HexagonalBinningVisitor.class);

    private double size;

    private double yoffset;

    private double xoffset;

    private double half_xoffset;

    private double yOrigin;

    private Coordinate center = new Coordinate();

    public enum HexType {
        Pointy, Flat
    }

    public HexagonalBinningVisitor(ReferencedEnvelope bbox, double size) {
        this.size = size;

        // width w = sqrt(3) * size : height h = 2 * size.
        // The horizontal distance between adjacent hexagon centers is w.
        // The vertical distance between adjacent hexagon centers is h * 3/4.
        this.yoffset = size * 1.5;
        this.xoffset = Math.sqrt(3.0) * size;
        this.half_xoffset = xoffset * 0.5;

        this.extent = new ReferencedEnvelope(bbox.getCoordinateReferenceSystem());
        extent.init(bbox.getMinX() - half_xoffset, bbox.getMaxX(), bbox.getMinY() - yoffset,
                bbox.getMaxY());

        this.minX = extent.getMinX();
        this.minY = extent.getMinY();
        this.yOrigin = minY + (size * 0.25);

        columns = (int) Math.floor((extent.getWidth() / xoffset) + 0.5d);
        rows = (int) Math.floor((extent.getHeight() / yoffset) + 0.5d);

        columns = columns * xoffset < extent.getWidth() ? columns + 1 : columns;
        rows = rows * yoffset < extent.getHeight() ? rows + 1 : rows;

        this.gridValues = new Double[rows][columns];

        this.binTemplate = this.createHexagon(minX, minY, size);
    }

    @Override
    public void visit(Coordinate coordinate, double value) {
        // origin = lower left
        int row = (int) Math.floor((coordinate.y - yOrigin) / yoffset);
        boolean even = (row % 2) == 1; // even row

        int col = -1;
        if (even) {
            col = (int) Math.floor((coordinate.x - minX - half_xoffset) / xoffset);
        } else {
            col = (int) Math.floor((coordinate.x - minX) / xoffset);
        }

        if (even) {
            center.setOrdinate(0, (minX + xoffset) + (col * xoffset));
        } else {
            center.setOrdinate(0, (minX + half_xoffset) + (col * xoffset));
        }
        center.setOrdinate(1, (minY + size) + (row * yoffset));

        double init_distance = coordinate.distance(center);
        if (init_distance > half_xoffset) {
            if (coordinate.x <= center.x) {
                // left cell
                Coordinate upper = createCenter(center.x - half_xoffset, center.y + yoffset);
                Coordinate lower = createCenter(center.x - half_xoffset, center.y - yoffset);

                double upper_distance = coordinate.distance(upper);
                double lower_distance = coordinate.distance(lower);
                if (upper_distance < init_distance || lower_distance < init_distance) {
                    col = even ? col : col - 1;
                    if (upper_distance > lower_distance) {
                        row = row - 1;
                    } else {
                        row = row + 1;
                    }
                }

            } else {
                // right cell
                Coordinate upper = createCenter(center.x + half_xoffset, center.y + yoffset);
                Coordinate lower = createCenter(center.x + half_xoffset, center.y - yoffset);

                double upper_distance = coordinate.distance(upper);
                double lower_distance = coordinate.distance(lower);
                if (upper_distance < init_distance || lower_distance < init_distance) {
                    col = even ? col + 1 : col;
                    if (upper_distance > lower_distance) {
                        row = row - 1;
                    } else {
                        row = row + 1;
                    }
                }
            }
        }

        if (col < 0 || row < 0 || col >= columns || row >= rows) {
            return;
        }

        Double preVal = gridValues[row][col];
        gridValues[row][col] = preVal == null ? value : preVal + value;

        if (getOnlyValidGrid()) {
            minCol = Math.min(col, minCol);
            maxCol = Math.max(col, maxCol);
            minRow = Math.min(row, minRow);
            maxRow = Math.max(row, maxRow);
        }
    }

    private Coordinate createCenter(double x, double y) {
        return new Coordinate(x, y);
    }

    @Override
    public Iterator<Bin> getBins(final GeometryCoordinateSequenceTransformer transformer) {
        if (!getOnlyValidGrid()) {
            minCol = 0;
            maxCol = columns;
            minRow = 0;
            maxRow = rows;
        }

        final int rowLimit = getOnlyValidGrid() ? maxRow + 1 : maxRow;
        final int colLimit = getOnlyValidGrid() ? maxCol + 1 : maxCol;

        return new Iterator<RectangularBinningVisitor.Bin>() {
            int featureID = 0;

            int row = minRow;

            int col = minCol;

            double ypos = minRow * yoffset;

            double xpos = (row % 2) == 1 ? (minCol * xoffset) + half_xoffset : minCol * xoffset;

            private void resetIndexes() {
                if (col == colLimit) {
                    row++;
                    ypos += yoffset;

                    xpos = minCol * xoffset;
                    col = minCol;
                    if ((row % 2) == 1) {
                        xpos += half_xoffset;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return row < rowLimit && col < colLimit;
            }

            @Override
            public Bin next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("hasNext() returned false!");
                }

                // Loop through the array: rows
                while (row < rowLimit) {
                    // Loop through the array: columns
                    while (col < colLimit) {
                        Double value = gridValues[row][col];
                        col++;

                        if (value == null && getOnlyValidGrid()) {
                            xpos += xoffset;
                            resetIndexes();
                        } else {
                            Geometry grid = (Geometry) binTemplate.copy();
                            grid.apply(new CoordinateTranslateFilter(xpos, ypos));

                            if (transformer != null) {
                                // reproject grid geometry to sourceCRS
                                grid = transform(transformer, grid);
                            }

                            xpos += xoffset;
                            resetIndexes();

                            return new Bin(featureID++, grid, value);
                        }
                    }
                }
                return null;
            }

            @Override
            public void remove() {
                // do nothing
            }
        };
    }

    private Geometry createHexagon(double minX, double minY, double size) {
        // center:top --> clockwise
        double span = Math.sqrt(3.0) * size;
        Coordinate[] ring = new Coordinate[7];
        ring[0] = new Coordinate(minX + 0.5 * span, minY + 2.0 * size);
        ring[1] = new Coordinate(minX + span, minY + 1.5 * size);
        ring[2] = new Coordinate(minX + span, minY + 0.5 * size);
        ring[3] = new Coordinate(minX + 0.5 * span, minY);
        ring[4] = new Coordinate(minX, minY + 0.5 * size);
        ring[5] = new Coordinate(minX, minY + 1.5 * size);
        ring[6] = ring[0];
        return gf.createPolygon(gf.createLinearRing(ring), null);
    }
}
