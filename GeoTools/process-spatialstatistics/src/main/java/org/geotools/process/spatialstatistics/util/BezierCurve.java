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

import org.geotools.process.spatialstatistics.styler.GraduatedColorStyleBuilder;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

/**
 * Converts LineString to Bezier Curve
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BezierCurve {
    protected static final Logger LOGGER = Logging.getLogger(GraduatedColorStyleBuilder.class);

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
        CoordinateList list = new CoordinateList();
        Coordinate[] coords = line.getCoordinates();

        if (useSegment) {
            double precision = 1.0 / quality;
            for (int i = 0; i < coords.length - 1; i++) {
                Coordinate from = coords[i];
                Coordinate to = coords[i + 1];
                Coordinate control = getControlPoint(new LineSegment(from, to));
                for (double step = 0; step < 1; step += precision) {
                    list.add(quadraticBezier(from, to, control, step), false);
                }
            }
            return line.getFactory().createLineString(list.toCoordinateArray());
        } else {
            return create(new LineSegment(coords[0], coords[coords.length - 1]));
        }
    }

    public LineString create(LineString line, Coordinate control) {
        double precision = 1.0 / quality;

        CoordinateList list = new CoordinateList();
        Coordinate[] coords = line.getCoordinates();

        if (useSegment) {
            for (int i = 0; i < coords.length - 1; i++) {
                Coordinate from = coords[i];
                Coordinate to = coords[i + 1];
                for (double step = 0; step < 1; step += precision) {
                    list.add(quadraticBezier(from, to, control, step), false);
                }
            }
            // add last coordinate
            list.add(coords[coords.length - 1], false);
            return line.getFactory().createLineString(list.toCoordinateArray());
        } else {
            Coordinate from = coords[0];
            Coordinate to = coords[coords.length - 1];
            return create(new LineSegment(from, to), control);
        }
    }

    private Coordinate quadraticBezier(Coordinate from, Coordinate to, Coordinate control, double t) {
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