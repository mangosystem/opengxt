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
package org.geotools.process.spatialstatistics.core;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Utility class for spatial statistics
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SSUtils {
    protected static final Logger LOGGER = Logging.getLogger(SSUtils.class);

    public enum StatEnum {
        LEFT,   // area under the curve to the left
        RIGHT,  // area under the curve to the right
        BOTH   // two-tailed test
    }

    /** Default tolerance for double comparisons: 1.0e-8 = 0.00000001 */
    public static final double DOUBLE_COMPARE_TOLERANCE = 0.00000001d;

    /** Default tolerance for float comparisons: 1.0e-4 = 0.0001 */
    public static final float FLOAT_COMPARE_TOLERANCE = 0.0001f;

    public static double zProb(double x, StatEnum type) {
        // Calculates the area under the curve of the standard normal distribution.
        // NOTES: Source - Algorithm AS 66: Applied Statistics, Vol. 22(3), 1973

        double pvalue = 0.0d;

        double x0 = 0.398942280444;
        double x1 = 0.39990348504;
        double x2 = 5.75885480458;
        double x3 = -29.8213557808;
        double x4 = 2.62433121679;
        double x5 = 48.6959930692;
        double x6 = 5.92885724438;
        double x7 = 0.398942280385;
        double x8 = -3.8052e-08;
        double x9 = 1.00000615302;
        double x10 = 3.98064794e-04;
        double x11 = 1.98615381364;
        double x12 = -0.151679116635;
        double x13 = 5.29330324926;
        double x14 = 4.8385912808;
        double x15 = -15.1508972451;
        double x16 = 0.742380924027;
        double x17 = 30.789933034;
        double x18 = 3.99019417011;
        double lt = 7.0;
        // double utz = 18.66;
        double con = 1.28;
        boolean pos = x >= 0;
        double z = Math.abs(x);

        if (z > lt) {
            pvalue = 0.0d;
        } else {
            double y = 0.5 * z * z;
            if (z <= con) {
                pvalue = 0.5 - z * (x0 - x1 * y / (y + x2 + x3 / (y + x4 + x5 / (y + x6))));
            } else {
                pvalue = x7
                        * Math.exp(-y)
                        / (z + x8 + x9
                                / (z + x10 + x11
                                        / (z + x12 + x13
                                                / (z + x14 + x15 / (z + x16 + x17 / (z + x18))))));
            }
        }

        switch (type) {
        case LEFT:
            if (pos)
                pvalue = 1.0 - pvalue;
            break;
        case RIGHT:
            if (!pos)
                pvalue = 1.0 - pvalue;
            break;
        default:
            pvalue = 2.0 * pvalue;
            break;
        }

        return pvalue;
    }

    public static double convert2Degree(double radians) {
        return radians * (180.0 / Math.PI);
    }

    public static double convert2Radians(double degree) {
        return Math.PI / 180.0 * degree;
    }

    public static double getAngle(double numerator, double denominator) {
        double ratio = 0.0;
        if (denominator == 0.0) {
            // #### 90 Degrees in Radians ####
            ratio = Math.PI / 2.0;
        } else if (numerator == 0.0) {
            // #### 180 Degrees in Radians ####
            ratio = Math.PI;
        } else {
            ratio = Math.abs(Math.atan(numerator / denominator));
        }

        // #### Quadrant Adjustment ####
        double angle = 0.0;
        if (numerator >= 0) {
            if (denominator >= 0) {
                // #### X and Y Positive (First Quadrant) ####
                angle = ratio;
            } else {
                // #### Y is Negative (Second Quadrant) ####
                angle = Math.PI - ratio;
            }
        } else {
            if (denominator < 0) {
                // #### X and Y Negative (Third Quadrant) ####
                angle = Math.PI + ratio;
            } else {
                // #### Y is Positive (Fourth Quadrant) ####
                angle = (2.0 * Math.PI) - ratio;
            }
        }

        return angle;
    }

    public static double getAngle(Point firstPoint, Point lastPoint) {
        return getAngle(firstPoint.getCoordinate(), lastPoint.getCoordinate());
    }

    public static double getAngle(Coordinate firstPoint, Coordinate lastPoint) {
        double numerator = lastPoint.x - firstPoint.x;
        double denominator = lastPoint.y - firstPoint.y;

        return getAngle(numerator, denominator);
    }

    public static double getEuclideanDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;

        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public static double getManhattanDistance(double x1, double y1, double x2, double y2) {
        // it is |x1 - x2| + |y1 - y2|.
        double dx = Math.abs(x1 - x2);
        double dy = Math.abs(y1 - y2);

        return dx + dy;
    }

    public static boolean compareDouble(double a, double b, double rTol) {
        // a (float): float to be compared
        // b (float): float to be compared
        // rTol (float): relative tolerance
        // aTol (float): absolute tolerance

        final double aTol = 0.00000001;

        // return (boolean): true if |a - b| < aTol + (rTol * |b|)

        if (Math.abs(a - b) < aTol + (rTol * Math.abs(b))) {
            return true;
        }

        return false;
    }

    public static boolean compareDouble(double a, double b) {
        return compareDouble(a, b, DOUBLE_COMPARE_TOLERANCE);
    }

    public static boolean compareFloat(float a, float b, float rTol) {
        // a (float): float to be compared
        // b (float): float to be compared
        // rTol (float): relative tolerance
        // aTol (float): absolute tolerance

        final float aTol = 0.0001f;

        if (Math.abs(a - b) < aTol + (rTol * Math.abs(b))) {
            return true;
        }

        return false;
    }

    public static boolean compareFloat(float a, float b) {
        return compareDouble(a, b, FLOAT_COMPARE_TOLERANCE);
    }
}
