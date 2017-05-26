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

import java.util.logging.Level;
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
        LEFT, // area under the curve to the left
        RIGHT, // area under the curve to the right
        BOTH // two-tailed test
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

    public static double fProb(double x, int m, int n, StatEnum type) {
        // Calculates the area under the curve for the F-distribution.
        int a, b, i, j;
        double w, z, p, y, d;

        a = 2 * (m / 2) - m + 2;
        b = 2 * (n / 2) - n + 2;
        w = x * ((double) m / (double) n);
        z = 1.0 / (1.0 + w);

        p = 0;
        y = 0.3183098862;

        if (a == 1) {
            if (b == 1) {
                p = Math.sqrt(w);
                d = y * z / p;
                p = 2.0 * y * Math.atan(p);
            } else {
                p = Math.sqrt(w * z);
                d = 0.5 * p * z / w;
            }
        } else {
            if (b == 1) {
                p = Math.sqrt(z);
                d = 0.5 * z * p;
                p = 1.0 - p;
            } else {
                d = z * z;
                p = w * z;
            }
        }

        y = 2.0 * w / z;
        j = b + 2;
        while (j <= n) {
            d = (1 + (1.0 * a) / (j - 2)) * d * z;
            if (a == 1) {
                p = p + d * y / (j - 1);
            } else {
                p = (p + w) * z;
            }
            j += 2;
        }

        y = w * z;
        z = 2.0 / z;
        b = n - 2;
        i = a + 2;
        while (i <= m) {
            j = i + b;
            d = y * d * j / (i - 2);
            p = p - z * d / j;
            i += 2;
        }

        if (type == StatEnum.LEFT) {
            p = 1.0 - p;
        }

        return p;
    }

    public static double tProb(double dof, double t, StatEnum type) {
        // Calculates the area under the curve of the studentized-t distribution.
        // NOTES: Source - Algorithm AS 66: Applied Statistics, Vol. 22(3), 1973

        if (dof <= 1) {
            LOGGER.log(Level.ALL, "Must hava more than One Degree of Freedom!");
            throw new IllegalArgumentException();
        }

        if (2 <= dof && dof <= 4) {
            LOGGER.log(Level.WARNING, "Degrees of Freedom is lesser than 5!");
        }

        double x1 = 0.09979441;
        double x2 = -0.581821;
        double x3 = 1.390993;
        double x4 = -1.222452;
        double x5 = 2.151185;
        double x6 = 5.537409;
        double x7 = 11.42343;
        double x8 = 0.04431742;
        double x9 = -0.2206018;
        double x10 = -0.03317253;
        double x11 = 5.679969;
        double x12 = -12.96519;
        double x13 = 5.166733;
        double x14 = 13.49862;
        double x15 = 0.009694901;
        double x16 = -0.1408854;
        double x17 = 1.88993;
        double x18 = -12.75532;
        double x19 = 25.77532;
        double x20 = 4.233736;
        double x21 = 14.3963;
        double x22 = -9.187228E-5;
        double x23 = 0.03789901;
        double x24 = -1.280346;
        double x25 = 9.249528;
        double x26 = -19.08115;
        double x27 = 2.777816;
        double x28 = 16.46132;
        double x29 = 5.79602E-4;
        double x30 = -0.02763334;
        double x31 = 0.4517029;
        double x32 = -2.657697;
        double x33 = 5.127212;
        double x34 = 0.5657187;
        double x35 = 21.83269;

        double V = 1.0 / dof;
        double abst = Math.abs(t);

        double tmp = 1.
                + abst
                * (((x1 + V * (x2 + V * (x3 + V * (x4 + V * x5)))) / (1 - V * (x6 - V * x7))) + abst
                        * (((x8 + V * (x9 + V * (x10 + V * (x11 + V * x12)))) / (1 - V
                                * (x13 - V * x14))) + abst
                                * (((x15 + V * (x16 + V * (x17 + V * (x18 + V * x19)))) / (1 - V
                                        * (x20 - V * x21))) + abst
                                        * (((x22 + V * (x23 + V * (x24 + V * (x25 + V * x26)))) / (1 - V
                                                * (x27 - V * x28))) + abst
                                                * ((x29 + V
                                                        * (x30 + V * (x31 + V * (x32 + V * x33)))) / (1 - V
                                                        * (x34 - V * x35)))))));

        double pValue = 0.5 * Math.pow(tmp, -8.0);

        if (type == StatEnum.LEFT) {
            if (t > 0) {
                pValue = 1.0 - pValue;
            }
        } else if (type == StatEnum.BOTH) {
            pValue = 2.0 * pValue;
        }

        return pValue;
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
            ratio = Math.PI / 2.0;
        } else if (numerator == 0.0) {
            ratio = Math.PI;
        } else {
            ratio = Math.abs(Math.atan(numerator / denominator));
        }

        double angle = 0.0;
        if (numerator >= 0) {
            if (denominator >= 0) {
                angle = ratio;
            } else {
                angle = Math.PI - ratio;
            }
        } else {
            if (denominator < 0) {
                angle = Math.PI + ratio;
            } else {
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
        final double aTol = 0.00000001;

        if (Math.abs(a - b) < aTol + (rTol * Math.abs(b))) {
            return true;
        }

        return false;
    }

    public static boolean compareDouble(double a, double b) {
        return compareDouble(a, b, DOUBLE_COMPARE_TOLERANCE);
    }

    public static boolean compareFloat(float a, float b, float rTol) {
        final float aTol = 0.0001f;

        if (Math.abs(a - b) < aTol + (rTol * Math.abs(b))) {
            return true;
        }

        return false;
    }

    public static boolean compareFloat(float a, float b) {
        return compareFloat(a, b, FLOAT_COMPARE_TOLERANCE);
    }
}
