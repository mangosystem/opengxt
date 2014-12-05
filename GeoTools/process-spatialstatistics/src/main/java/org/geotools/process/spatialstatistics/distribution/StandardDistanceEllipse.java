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
package org.geotools.process.spatialstatistics.distribution;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;

/**
 * StandardDistanceEllipse
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDistanceEllipse {
    protected static final Logger LOGGER = Logging.getLogger(StandardDistanceEllipse.class);

    private double sumX = 0.0;

    private double sumY = 0.0;

    private double sumZ = 0.0;

    private double weightSum = 0.0;

    private List<Event> events = new ArrayList<Event>();

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    class Event {
        public double x;

        public double y;

        public double weight;

        public Event() {
        }

        public Event(double x, double y, double weight) {
            this.x = x;
            this.y = y;
            this.weight = weight;
        }
    }

    public void addValue(Coordinate coordinate, double weight) {
        weightSum += weight;

        sumX += coordinate.x * weight;
        sumY += coordinate.y * weight;
        sumZ += coordinate.z * weight;

        events.add(new Event(coordinate.x, coordinate.y, weight));
    }

    public double seX = 0;

    public double seY = 0;

    public double radianRotation2 = 0.0;

    public Geometry ellipseCircle = null;

    public Geometry calculateSDE(double stdDeviations) {
        if (ellipseCircle != null) {
            return ellipseCircle;
        }

        // Mean Center
        double meanX = sumX / weightSum;
        double meanY = sumY / weightSum;

        // Standard Ellipse
        double sigXY = 0, sigX = 0, sigY = 0;
        for (Event ce : events) {
            final double devX = ce.x - meanX;
            final double devY = ce.y - meanY;

            sigX += Math.pow(devX, 2.0) * ce.weight;
            sigY += Math.pow(devY, 2.0) * ce.weight;
            sigXY += devX * devY * ce.weight;
        }

        double denom = sigXY * 2.0;
        double diffXY = sigX - sigY;
        double sum1 = Math.pow(diffXY, 2.0) + 4.0 * Math.pow(sigXY, 2.0);

        double arctanVal = 0.0;
        if (Math.abs(denom) > 0) {
            double tempVal = (diffXY + Math.sqrt(sum1)) / denom;
            arctanVal = Math.atan(tempVal);
        }

        if (arctanVal < 0.0) {
            arctanVal += (Math.PI / 2.0);
        }

        double sinVal = Math.sin(arctanVal);
        double cosVal = Math.cos(arctanVal);
        double sqrt2 = Math.sqrt(2.0);
        double sigXYSinCos = 2.0 * sigXY * sinVal * cosVal;

        seX = (sqrt2
                * Math.sqrt(((sigX * Math.pow(cosVal, 2.0)) - sigXYSinCos + (sigY * Math.pow(
                        sinVal, 2.0))) / weightSum) * stdDeviations);
        seY = (sqrt2
                * Math.sqrt(((sigX * Math.pow(sinVal, 2.0)) + sigXYSinCos + (sigY * Math.pow(
                        cosVal, 2.0))) / weightSum) * stdDeviations);

        // Counter Clockwise from Noon
        double degreeRotation = 360.0 - SSUtils.convert2Degree(arctanVal);

        // Convert to Radians
        double radianRotation1 = SSUtils.convert2Radians(degreeRotation);

        // Add Rotation
        radianRotation2 = 360.0 - degreeRotation;
        if (seX > seY) {
            radianRotation2 += 90.0;
            if (radianRotation2 > 360.0) {
                radianRotation2 = radianRotation2 - 180.0;
            }
        }

        // Calculate a Point For Each Degree in Ellipse Polygon
        double seX2 = Math.pow(seX, 2.0);
        double seY2 = Math.pow(seY, 2.0);
        double cosRadian = Math.cos(radianRotation1);
        double sinRadian = Math.sin(radianRotation1);

        List<Coordinate> coordList = new ArrayList<Coordinate>();
        for (int degree = 0; degree <= 360; degree++) {
            double radians = SSUtils.convert2Radians(degree);
            double tanVal2 = Math.pow(Math.tan(radians), 2.0);
            double dX = Math.sqrt((seX2 * seY2) / (seY2 + (seX2 * tanVal2)));
            double dY = Math.sqrt((seY2 * (seX2 - Math.pow(dX, 2.0))) / seX2);

            // Adjust for Quadrant
            if (degree >= 90 && degree < 180) {
                dX = -dX;
            } else if (degree >= 180 && degree < 270) {
                dX = -dX;
                dY = -dY;
            } else if (degree >= 270) {
                dY = -dY;
            }

            // Rotate X and Y
            double dXr = dX * cosRadian - dY * sinRadian;
            double dYr = dX * sinRadian + dY * cosRadian;

            // Create Point Shifted to Ellipse Centroid
            coordList.add(new Coordinate(dXr + meanX, dYr + meanY));
        }

        try {
            if (!coordList.get(0).equals(coordList.get(coordList.size() - 1))) {
                coordList.add(coordList.get(0));
            }
            Coordinate[] coords = CoordinateArrays.toCoordinateArray(coordList);
            LinearRing ring = gf.createLinearRing(coords);
            ellipseCircle = gf.createPolygon(ring, null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            ellipseCircle = null;
        }

        return ellipseCircle;
    }

    public Point getMeanCenter() {
        double meanX = sumX / weightSum;
        double meanY = sumY / weightSum;
        double meanZ = sumZ / weightSum;

        return gf.createPoint(new Coordinate(meanX, meanY, meanZ));
    }

}
