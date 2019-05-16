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

import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * StandardDistance
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDistance {
    private double sumX = 0.0;

    private double sumY = 0.0;

    private double sumZ = 0.0;

    private double weightSum = 0.0;

    @SuppressWarnings("unused")
    private int numFeatures = 0;

    private double sdVal = Double.NaN;

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

        numFeatures++;
    }

    public double getStdDist(double standardDeviation) {
        if (Double.isNaN(sdVal)) {
            double meanX = sumX / weightSum;
            double meanY = sumY / weightSum;

            double sigXYSum = 0;
            for (Event ce : events) {
                final double devX = ce.x - meanX;
                final double devY = ce.y - meanY;

                sigXYSum += (Math.pow(devX, 2.0) * ce.weight) + (Math.pow(devY, 2.0) * ce.weight);
            }

            sdVal = Math.sqrt(sigXYSum / weightSum) * standardDeviation;
        }

        return sdVal;
    }

    public Point getMeanCenter() {
        double meanX = sumX / weightSum;
        double meanY = sumY / weightSum;
        double meanZ = sumZ / weightSum;

        return gf.createPoint(new Coordinate(meanX, meanY, meanZ));
    }

}
