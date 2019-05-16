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
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Circular Binning Visitor.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class CircularBinningVisitor extends AbstractBinningVisitor {
    protected static final Logger LOGGER = Logging.getLogger(CircularBinningVisitor.class);

    private double diameter;

    private int quadrantSegments = 16; // default = 32

    public int getQuadrantSegments() {
        return quadrantSegments;
    }

    public void setQuadrantSegments(int quadrantSegments) {
        this.quadrantSegments = quadrantSegments < 8 ? 8 : quadrantSegments;
    }

    public CircularBinningVisitor(ReferencedEnvelope bbox, double radius) {
        this.diameter = radius * 2.0;

        columns = (int) Math.floor((bbox.getWidth() / diameter) + 0.5d);
        rows = (int) Math.floor((bbox.getHeight() / diameter) + 0.5d);

        this.columns = columns * diameter < bbox.getWidth() ? columns + 1 : columns;
        this.rows = rows * diameter < bbox.getHeight() ? rows + 1 : rows;

        // recalculate envelope : origin = lower left
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(targetCRS);
        finalBBox.init(bbox.getMinX(), bbox.getMinX() + (columns * diameter), bbox.getMinY(),
                bbox.getMinY() + (rows * diameter));

        this.extent = finalBBox;

        this.minX = finalBBox.getMinX();
        this.minY = finalBBox.getMinY();

        this.gridValues = new Double[rows][columns];

        Point center = gf.createPoint(new Coordinate(minX + radius, minY + radius));
        this.binTemplate = center.buffer(radius, quadrantSegments);
    }

    @Override
    public void visit(Coordinate coordinate, double value) {
        // origin = lower left
        int col = (int) Math.floor((coordinate.x - minX) / diameter);
        int row = (int) Math.floor((coordinate.y - minY) / diameter);
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

            double ypos = minRow * diameter;

            double xpos = minCol * diameter;

            private void resetIndexes() {
                if (col == colLimit) {
                    xpos = minCol * diameter;
                    col = minCol;

                    row++;
                    ypos += diameter;
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
                            xpos += diameter;
                            resetIndexes();
                        } else {
                            Geometry grid = (Geometry) binTemplate.copy();
                            grid.apply(new CoordinateTranslateFilter(xpos, ypos));

                            if (transformer != null) {
                                // reproject grid geometry to sourceCRS
                                grid = transform(transformer, grid);
                            }

                            xpos += diameter;
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
}
