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
package org.geotools.process.spatialstatistics.util;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearGeometryBuilder;

/**
 * Converts LineString to Bezier Curve
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BezierCurve {
    protected static final Logger LOGGER = Logging.getLogger(BezierCurve.class);

    private int quality = 24; // default

    private double offsetDegree = 25; // default

    private double fraction = 0.75; // default

    private boolean useSegment = false; // default

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public double getOffsetDegree() {
        return offsetDegree;
    }

    public void setOffsetDegree(double offsetDegree) {
        this.offsetDegree = offsetDegree;
    }

    public double getFraction() {
        return fraction;
    }

    public void setFraction(double fraction) {
        this.fraction = fraction;
    }

    public boolean isUseSegment() {
        return useSegment;
    }

    public void setUseSegment(boolean useSegment) {
        this.useSegment = useSegment;
    }

    public BezierCurve() {

    }

    public LineString create(LineSegment lineSeg) {
        return create(lineSeg, getControlPoint(lineSeg));
    }

    public LineString create(LineSegment lineSeg, Coordinate control) {
        double precision = 1.0 / quality;

        CoordinateList coords = new CoordinateList();
        for (double step = 0; step < 1; step += precision) {
            coords.add(quadraticBezier(lineSeg.p0, lineSeg.p1, control, step), false);
        }

        return new GeometryFactory().createLineString(coords.toCoordinateArray());
    }

    public LineString create(LineString line) {
        LinearGeometryBuilder builder = new LinearGeometryBuilder(line.getFactory());
        builder.setFixInvalidLines(true);

        Coordinate[] coords = line.getCoordinates();
        if (useSegment) {
            double precision = 1.0 / quality;
            for (int i = 0; i < coords.length - 1; i++) {
                Coordinate from = coords[i];
                Coordinate to = coords[i + 1];
                Coordinate control = getControlPoint(new LineSegment(from, to));
                for (double step = 0; step < 1; step += precision) {
                    builder.add(quadraticBezier(from, to, control, step), false);
                }
            }
            return (LineString) builder.getGeometry();
        } else {
            return create(new LineSegment(coords[0], coords[coords.length - 1]));
        }
    }

    public LineString create(LineString line, Coordinate control) {
        double precision = 1.0 / quality;

        LinearGeometryBuilder builder = new LinearGeometryBuilder(line.getFactory());
        builder.setFixInvalidLines(true);

        Coordinate[] coords = line.getCoordinates();
        if (useSegment) {
            for (int i = 0; i < coords.length - 1; i++) {
                Coordinate from = coords[i];
                Coordinate to = coords[i + 1];
                for (double step = 0; step < 1; step += precision) {
                    builder.add(quadraticBezier(from, to, control, step), false);
                }
            }
            // add last coordinate
            builder.add(coords[coords.length - 1], false);
            return (LineString) builder.getGeometry();
        } else {
            Coordinate from = coords[0];
            Coordinate to = coords[coords.length - 1];
            return create(new LineSegment(from, to), control);
        }
    }

    private Coordinate quadraticBezier(Coordinate from, Coordinate to, Coordinate control,
            double t) {
        double x = (1 - t) * (1 - t) * from.x + (2 - 2 * t) * t * control.x + t * t * to.x;
        double y = (1 - t) * (1 - t) * from.y + (2 - 2 * t) * t * control.y + t * t * to.y;

        return new Coordinate(x, y);
    }

    private Coordinate getControlPoint(LineSegment lineSeg) {
        double radius = lineSeg.getLength() * fraction;
        double radian = lineSeg.angle() + Math.toRadians(offsetDegree);

        double dx = lineSeg.p0.x + (Math.cos(radian) * radius);
        double dy = lineSeg.p0.y + (Math.sin(radian) * radius);
        return new Coordinate(dx, dy);
    }
}