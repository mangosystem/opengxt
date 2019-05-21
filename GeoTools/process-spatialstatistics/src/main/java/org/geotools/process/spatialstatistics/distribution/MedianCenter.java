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

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Median Center
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MedianCenter {
    private double sumX = 0.0;

    private double sumY = 0.0;

    private double minX = Double.MAX_VALUE;

    private double minY = Double.MAX_VALUE;

    private double maxX = Double.MIN_VALUE;

    private double maxY = Double.MIN_VALUE;

    private double weightSum = 0.0;

    private Point medianCenter = null;

    private List<Event> events = new ArrayList<Event>();

    private final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools
            .getDefaultHints());

    class Event {
        public double x;

        public double y;

        public double weight;

        public Number[] attVals;

        public Event() {
        }

        public Event(double x, double y, double weight, Number[] attVals) {
            this.x = x;
            this.y = y;
            this.weight = weight;
            this.attVals = attVals;
        }

        public double getEuclideanDistance(Event other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.sqrt((dx * dx) + (dy * dy));
        }
    }

    public void addValue(Coordinate coordinate, double weight, Number[] attVals) {
        weightSum += weight;
        sumX += coordinate.x * weight;
        sumY += coordinate.y * weight;

        minX = Math.min(minX, coordinate.x);
        minY = Math.min(minY, coordinate.y);

        maxX = Math.max(maxX, coordinate.x);
        maxY = Math.max(maxY, coordinate.y);

        events.add(new Event(coordinate.x, coordinate.y, weight, attVals));
    }

    public Point getMedianCenter() {
        if (medianCenter != null) {
            return medianCenter;
        }

        final int maxIters = 100; // maximum number of iterations
        final double tolerance = 0.000001;

        Event med = new Event();
        med.x = sumX / weightSum;
        med.y = sumY / weightSum;

        if (events.size() == 1) {
            return gf.createPoint(new Coordinate(med.x, med.y));
        }

        double width = maxX - minX;
        double height = maxY - minY;

        double deltaX = maxX - minX;
        maxX = maxX + (deltaX / 1000.0);

        double deltaY = maxY - minY;
        minY = minY - (deltaY / 1000.0);

        height = maxY - minY;
        width = maxX - minX;

        double extentArea = height * width * 1000.0;

        boolean flag = true;
        int iterations = 0;
        while (flag) {

            if (iterations++ >= maxIters) {
                flag = false;
                break;
            }

            Event newCenter = evaluateDistance(med, extentArea);

            boolean diffX = SSUtils.compareDouble(newCenter.x, med.x, tolerance);
            boolean diffY = SSUtils.compareDouble(newCenter.y, med.y, tolerance);
            if (diffX && diffY) {
                flag = false;
            } else {
                med.x = newCenter.x;
                med.y = newCenter.y;
            }
        }

        medianCenter = gf.createPoint(new Coordinate(med.x, med.y));

        return medianCenter;
    }

    public Number[] getUnivariateMedian() {
        Event initCe = events.get(0);
        if (initCe.attVals == null) {
            return null;
        }

        int size = initCe.attVals.length;
        MedianVisitor[] visitor = new MedianVisitor[size];
        for (int k = 0; k < visitor.length; k++) {
            visitor[k] = new MedianVisitor();
        }

        for (Event ce : events) {
            for (int k = 0; k < size; k++) {
                visitor[k].visit(ce.attVals[k]);
            }
        }

        Number[] medianValue = new Number[size];
        for (int k = 0; k < visitor.length; k++) {
            medianValue[k] = visitor[k].getMedian();
        }

        return medianValue;
    }

    private Event evaluateDistance(Event estimate, double maxK) {
        double sumK = 0;
        double newXTop = 0;
        double newYTop = 0;

        for (Event ce : events) {
            final double dij = estimate.getEuclideanDistance(ce);
            double k = 0;

            if (dij == 0) {
                k = ce.weight * maxK;
            } else {
                k = ce.weight / dij;
            }

            sumK += k;
            newXTop += k * ce.x;
            newYTop += k * ce.y;
        }

        return new Event(newXTop / sumK, newYTop / sumK, 1.0, null);
    }
}