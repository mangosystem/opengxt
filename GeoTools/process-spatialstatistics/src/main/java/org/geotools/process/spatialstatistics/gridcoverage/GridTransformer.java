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

import java.awt.geom.AffineTransform;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.DirectPosition;

/**
 * Grid Transformer
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
/**
 * @author mappl
 *
 */
public class GridTransformer {
    protected static final Logger LOGGER = Logging.getLogger(GridTransformer.class);

    private Envelope extent;

    private int columns;

    private int rows;

    private double dx;

    private double dy;

    private double half_dx;

    private double half_dy;

    /**
     * Creates a new transform.
     * 
     * @param extent the envelope defining one coordinate system
     * @param dx the x size of the grid cell
     * @param dy the y size of the grid cell
     */
    public GridTransformer(Envelope extent, double dx, double dy) {
        init(extent, dx, dy);
    }

    public GridTransformer(GridCoverage2D coverage) {
        this(coverage.getGridGeometry());
    }

    public GridTransformer(GridGeometry2D gridGeometry2D) {
        ReferencedEnvelope extent = new ReferencedEnvelope(gridGeometry2D.getEnvelope());
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        init(extent, Math.abs(gridToWorld.getScaleX()), Math.abs(gridToWorld.getScaleY()));
    }

    private void init(Envelope extent, double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
        this.half_dx = dx / 2.0;
        this.half_dy = dy / 2.0;

        // calculate columns & rows
        this.columns = (int) Math.floor((extent.getWidth() / dx) + 0.5);
        this.rows = (int) Math.floor((extent.getHeight() / dy) + 0.5);

        // recalculate extent
        final double maxX = extent.getMinX() + (columns * dx);
        final double maxY = extent.getMinY() + (rows * dy);

        this.extent = new Envelope(extent.getMinX(), maxX, extent.getMinY(), maxY);
    }

    public Envelope getExtent() {
        return this.extent;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    /**
     * Checks whether or not this grid contains the specified grid location.
     * 
     * @param column the index of a grid column
     * @param row the index of a grid row
     * @return true if the specified location is inside this grid; false otherwise.
     */
    public boolean contains(int column, int row) {
        return (column >= 0 && column < columns) && (row >= 0 && row < rows);
    }

    /**
     * Checks whether or not this grid contains the specified world location.
     * 
     * @param x the X coordinate of the world location
     * @param y the Y coordinate of the world location
     * @return true if the specified location is inside this grid; false otherwise.
     */
    public boolean contains(double x, double y) {
        return extent.contains(x, y);
    }

    /**
     * Computes the X coordinate of the grid column.
     * 
     * @param column the index of a grid column
     * @return the X coordinate of the column
     */
    public double getX(int column) {
        return extent.getMinX() + (column * dx) + half_dx;
    }

    /**
     * Computes the Y coordinate of the grid row.
     * 
     * @param row the index of a grid row
     * @return the Y coordinate of the row
     */
    public double getY(int row) {
        return extent.getMaxY() - ((row * dy) + half_dy);
    }

    /**
     * Computes the column index of an X coordinate.
     * 
     * @param x the X coordinate
     * @return the column index
     */
    public int getColumn(double x) {
        final double diff = x - extent.getMinX();
        if (diff >= 0) {
            return (int) Math.floor(diff / dx);
        } else {
            return (int) Math.floor((diff / dx) - 0.5);
        }
    }

    /**
     * Computes the row index of an Y coordinate.
     * 
     * @param y the Y coordinate
     * @return the row index
     */
    public int getRow(double y) {
        final double diff = extent.getMaxY() - y;
        if (diff >= 0) {
            return (int) Math.floor(diff / dy);
        } else {
            return (int) Math.floor((diff / dy) - 0.5);
        }
    }

    /**
     * Transforms a point represented by a DirectPosition object from grid to world coordinates. The coordinates returned are those of the centre of
     * the grid cell in which the point lies.
     * 
     * @param gridPos The point in grid coordinate system.
     * @return world DirectPosition
     */
    public DirectPosition gridToWorld(DirectPosition gridPos) {
        return gridToWorld((int) gridPos.getOrdinate(0), (int) gridPos.getOrdinate(1));
    }

    /**
     * Transforms a point represented by column, row index from grid to world coordinates. The coordinates returned are those of the centre of the
     * grid cell in which the point lies.
     * 
     * @param column the index of a grid column
     * @param row the index of a grid row
     * @return world DirectPosition
     */
    public DirectPosition gridToWorld(int column, int row) {
        return new DirectPosition2D(getX(column), getY(row));
    }

    /**
     * Transforms a point represented by column, row index from grid to world coordinates. The coordinates returned are those of the centre of the
     * grid cell in which the point lies.
     * 
     * @param column the index of a grid column
     * @param row the index of a grid row
     * @return world Coordinate
     */
    public Coordinate gridToWorldCoordinate(int column, int row) {
        return new Coordinate(getX(column), getY(row));
    }

    /**
     * Transforms a point represented by a GridCoordinates2D object from grid to world coordinates. The coordinates returned are those of the centre
     * of the grid cell in which the point lies.
     * 
     * @param realPos The point in world coordinate system.
     * @return DirectPosition
     */
    public GridCoordinates2D worldToGrid(DirectPosition realPos) {
        return worldToGrid(realPos.getOrdinate(0), realPos.getOrdinate(1));
    }

    /**
     * Transforms a point represented by a GridCoordinates2D object from grid to world coordinates. The coordinates returned are those of the centre
     * of the grid cell in which the point lies.
     * 
     * @param realPos The Point in world coordinate system.
     * @return DirectPosition
     */
    public GridCoordinates2D worldToGrid(Point realPos) {
        return worldToGrid(realPos.getX(), realPos.getY());
    }

    /**
     * Transforms a point represented by a GridCoordinates2D object from grid to world coordinates. The coordinates returned are those of the centre
     * of the grid cell in which the point lies.
     * 
     * @param realPos The Coordinate in world coordinate system.
     * @return DirectPosition
     */
    public GridCoordinates2D worldToGrid(Coordinate realPos) {
        return worldToGrid(realPos.x, realPos.y);
    }

    /**
     * Transforms a point represented by a GridCoordinates2D object from grid to world coordinates. The coordinates returned are those of the centre
     * of the grid cell in which the point lies.
     * 
     * @param x the X coordinate
     * @param y the Y coordinate
     * @return DirectPosition
     */
    public GridCoordinates2D worldToGrid(double x, double y) {
        int column = getColumn(x);
        int row = getRow(y);
        return new GridCoordinates2D(column, row);
    }

    public java.awt.Rectangle worldToGridBounds(ReferencedEnvelope envelope) {
        int x = getColumn(envelope.getMinX());
        int y = getRow(envelope.getMaxY());

        int width = (int) Math.ceil((envelope.getWidth() / dx) + 0.5);
        int height = (int) Math.ceil((envelope.getHeight() / dy) + 0.5);

        if (x < 0) {
            width += x;
            x = 0;
        } else if (x >= columns) {
            x = columns;
            width = 0;
        }

        if (y < 0) {
            height += y;
            y = 0;
        } else if (y >= rows) {
            y = rows;
            height = 0;
        }

        if ((x + width) > columns) {
            width = columns - x;
        }

        if ((y + height) > rows) {
            height = rows - y;
        }

        return new java.awt.Rectangle(x, y, width, height);
    }
}