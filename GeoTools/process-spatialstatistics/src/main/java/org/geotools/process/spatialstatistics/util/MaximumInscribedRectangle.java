/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.process.spatialstatistics.util;

import java.util.Stack;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Constructs the Maximum Inscribed Rectangle.
 * 
 * @reference https://www.drdobbs.com/database/the-maximal-rectangle-problem/184410529
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MaximumInscribedRectangle {
    protected static final Logger LOGGER = Logging.getLogger(MaximumInscribedRectangle.class);

    /**
     * Gets the maximum inscribed rectangle
     * 
     * @return Polygon geometry
     */
    public static Geometry getRectangle(Geometry geom, boolean rotate) {
        return (new MaximumInscribedRectangle(geom, rotate)).getRectangle();
    }

    /**
     * Gets a line representing a diameter of the maximum inscribed rectangle.
     * 
     * @return LineString geometry
     */
    public static Geometry getDiameterLine(Geometry geom, boolean rotate) {
        return (new MaximumInscribedRectangle(geom, rotate)).getDiameterLine();
    }

    private boolean rotate = true;

    private Geometry inputGeometry;

    private GeometryFactory factory;

    private IndexedPointInAreaLocator locator;

    private Polygon maximumRectangle;

    public MaximumInscribedRectangle(Geometry polygonal) {
        this(polygonal, true);
    }

    public MaximumInscribedRectangle(Geometry polygonal, boolean rotate) {
        if (polygonal.isEmpty()) {
            throw new IllegalArgumentException("Empty input geometry is not supported");
        }

        if (!(polygonal instanceof Polygon || polygonal instanceof MultiPolygon)) {
            throw new IllegalArgumentException("Input geometry must be a Polygon or MultiPolygon");
        }

        this.rotate = rotate;
        this.factory = polygonal.getFactory();

        if (polygonal instanceof MultiPolygon && polygonal.getNumGeometries() > 1) {
            Geometry maxAreaGeometry = (Polygon) polygonal.getGeometryN(0);
            for (int index = 1; index < polygonal.getNumGeometries(); index++) {
                Polygon polygon = (Polygon) polygonal.getGeometryN(index);
                if (polygon.getArea() > maxAreaGeometry.getArea()) {
                    maxAreaGeometry = polygon;
                }
            }
            this.inputGeometry = maxAreaGeometry;
        } else {
            this.inputGeometry = polygonal;
        }
    }

    public Geometry getRectangle() {
        compute();

        return this.maximumRectangle;
    }

    public Geometry getDiameterLine() {
        compute();

        Coordinate[] coords = this.maximumRectangle.getCoordinates();
        LineSegment seg1 = new LineSegment(coords[0], coords[1]);
        LineSegment seg2 = new LineSegment(coords[1], coords[2]);

        Coordinate p1;
        Coordinate p2;

        if (rotate) {
            if (seg1.getLength() < seg2.getLength()) {
                p1 = seg1.midPoint();
                p2 = LineSegment.midPoint(coords[2], coords[3]);
            } else {
                p1 = seg2.midPoint();
                p2 = LineSegment.midPoint(coords[3], coords[4]);
            }
        } else {
            p1 = seg1.midPoint();
            p2 = LineSegment.midPoint(coords[2], coords[3]);
        }

        if (p1.getX() < p2.getX()) {
            return factory.createLineString(new Coordinate[] { p1, p2 });
        } else {
            return factory.createLineString(new Coordinate[] { p2, p1 });
        }
    }

    private void compute() {
        // check if computation is cached
        if (maximumRectangle != null) {
            return;
        }

        Envelope extent = null;
        Coordinate anchor = null;

        double theta = 0;
        AffineTransformation trans = null;

        if (rotate) {
            MinimumDiameter min = new MinimumDiameter(inputGeometry);
            Geometry minimumRec = min.getMinimumRectangle();
            LineString diameter = min.getDiameter();

            Coordinate start = diameter.getStartPoint().getCoordinate();
            Coordinate end = diameter.getEndPoint().getCoordinate();
            theta = start.x - end.x < 0 ? Angle.angle(start, end) : Angle.angle(end, start);

            anchor = minimumRec.getCentroid().getCoordinate();

            trans = new AffineTransformation();
            trans.setToIdentity();
            trans.rotate(-theta, anchor.x, anchor.y);

            Geometry ratoted = trans.transform(inputGeometry);
            extent = ratoted.getEnvelopeInternal();

            this.locator = new IndexedPointInAreaLocator(ratoted);
        } else {
            extent = inputGeometry.getEnvelopeInternal();
            this.locator = new IndexedPointInAreaLocator(inputGeometry);
        }

        final int scale = 40;

        final double dx = extent.getWidth() / scale;
        final double dy = extent.getHeight() / scale;
        final double halfDx = dx / 2.0;
        final double halfDy = dy / 2.0;

        final int columns = (int) Math.ceil(extent.getWidth() / dx);
        final int rows = (int) Math.ceil(extent.getHeight() / dy);

        // create cell matrix
        boolean[][] matrix = new boolean[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                double x = extent.getMinX() + (column * dx) + halfDx;
                double y = extent.getMinY() + (row * dy) + halfDy;
                Coordinate coord = new Coordinate(x, y);

                matrix[row][column] = Location.INTERIOR == locator.locate(coord);
            }
        }

        // find maximum inscribed rectangle
        findMaximumRectangle(matrix, extent, dx, dy, columns, rows);

        // rotate rectangle
        if (rotate && trans != null) {
            trans.setToIdentity();
            trans.rotate(theta, anchor.x, anchor.y);
            this.maximumRectangle = (Polygon) trans.transform(this.maximumRectangle);
        }
    }

    private void findMaximumRectangle(boolean[][] matrix, Envelope extent, double dx, double dy,
            int columns, int rows) {
        Cell lowerLeft = new Cell(0, 0);
        Cell upperRight = new Cell(-1, -1);
        int maximumArea = 0;

        Stack<Cell> stack = new Stack<Cell>();
        int[] cache = new int[columns + 1];

        for (int row = 0; row != rows; row++) {
            int openWidth = 0;
            cache = updateCache(cache, matrix[row]);
            for (int column = 0; column != columns + 1; column++) {
                if (cache[column] > openWidth) {
                    stack.push(new Cell(column, openWidth));
                    openWidth = cache[column];
                } else if (cache[column] < openWidth) {
                    Cell cell = null;
                    do {
                        cell = stack.pop();
                        int area = openWidth * (column - cell.x);
                        if (area > maximumArea) {
                            maximumArea = area;
                            lowerLeft.setXY(cell.x, row);
                            upperRight.setXY(column - 1, row - openWidth + 1);
                        }
                        openWidth = cell.y;
                    } while (cache[column] < openWidth);

                    openWidth = cache[column];
                    if (openWidth != 0) {
                        stack.push(cell);
                    }
                }
            }
        }

        // build Rectangle
        double minX = extent.getMinX() + (lowerLeft.x * dx) + dx;
        double minY = extent.getMinY() + (lowerLeft.y * dy) + dy;

        double maxX = extent.getMinX() + (upperRight.x * dx) + dx;
        double maxY = extent.getMinY() + (upperRight.y * dy) + dy;

        Envelope rectangle = new Envelope(minX, maxX, minY, maxY);
        if (rectangle.getArea() == 0) {
            MaximumInscribedCircle mic = new MaximumInscribedCircle(inputGeometry, 1.0);
            Geometry circle = mic.getCenter().buffer(mic.getRadiusLine().getLength());
            rectangle = circle.getEnvelopeInternal();
        }

        this.maximumRectangle = (Polygon) factory.toGeometry(rectangle);
    }

    private int[] updateCache(int[] cache, boolean[] matrixRow) {
        for (int column = 0; column < matrixRow.length; column++) {
            if (matrixRow[column]) {
                cache[column]++;
            } else {
                cache[column] = 0;
            }
        }
        return cache;
    }

    static class Cell {

        private int x;

        private int y;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setXY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
