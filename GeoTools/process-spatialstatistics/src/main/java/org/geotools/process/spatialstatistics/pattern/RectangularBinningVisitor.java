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

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.util.CoordinateTranslateFilter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Rectangular Binning Visitor.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class RectangularBinningVisitor extends AbstractBinningVisitor {
    protected static final Logger LOGGER = Logging.getLogger(RectangularBinningVisitor.class);

    private double width;

    private double height;

    public RectangularBinningVisitor(ReferencedEnvelope bbox, double width, double height) {
        int columns = (int) Math.floor((bbox.getWidth() / width) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / height) + 0.5d);

        columns = columns * width < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * height < bbox.getHeight() ? rows + 1 : rows;

        // recalculate envelope : origin = lower left
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(targetCRS);
        finalBBox.init(bbox.getMinX(), bbox.getMinX() + (columns * width), bbox.getMinY(),
                bbox.getMinY() + (rows * height));

        this.init(finalBBox, columns, rows);
    }

    public RectangularBinningVisitor(ReferencedEnvelope bbox, int columns, int rows) {
        this.init(bbox, columns, rows);
    }

    private void init(ReferencedEnvelope bbox, int columns, int rows) {
        this.columns = columns;
        this.rows = rows;

        this.width = bbox.getWidth() / columns;
        this.height = bbox.getHeight() / rows;

        this.extent = bbox;

        this.minX = bbox.getMinX();
        this.minY = bbox.getMinY();

        this.gridValues = new Double[rows][columns];

        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope bounds = new ReferencedEnvelope(targetCRS);
        bounds.init(minX, minX + width, minY, minY + height);

        this.binTemplate = gf.toGeometry(bounds);
    }

    @Override
    public void visit(Coordinate coordinate, double value) {
        // origin = lower left
        int col = (int) Math.floor((coordinate.x - minX) / width);
        int row = (int) Math.floor((coordinate.y - minY) / height);
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

            double ypos = minRow * height;

            double xpos = minCol * width;

            private void resetIndexes() {
                if (col == colLimit) {
                    xpos = minCol * width;
                    col = minCol;

                    row++;
                    ypos += height;
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
                            xpos += width;
                            resetIndexes();
                        } else {
                            Geometry grid = (Geometry) binTemplate.copy();
                            grid.apply(new CoordinateTranslateFilter(xpos, ypos));

                            if (transformer != null) {
                                // reproject grid geometry to sourceCRS
                                grid = transform(transformer, grid);
                            }

                            xpos += width;
                            resetIndexes();

                            // NULL to zero
                            value = value == null ? Double.valueOf(0d) : value;

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
}
