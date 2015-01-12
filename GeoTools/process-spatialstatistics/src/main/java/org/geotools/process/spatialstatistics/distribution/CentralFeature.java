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
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * CentralFeature
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CentralFeature {

    class Event {
        int oid;

        public double x;

        public double y;

        public double weight = 1.0;

        public double potential = 0.0;

        public Event() {
        }

        public Event(int oid, double x, double y, double weight, double potential) {
            this.oid = oid;
            this.x = x;
            this.y = y;
            this.weight = weight;
            this.potential = potential;
        }
    }

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    private int numFeatures = 0;

    private List<Event> events = new ArrayList<Event>();

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public void addValue(Coordinate coordinate, double weight, double potential) {
        events.add(new Event(numFeatures, coordinate.x, coordinate.y, weight, potential));
        numFeatures++;
    }

    public void addValue(Point point, double weight, double potential) {
        addValue(point.getCoordinate(), weight, potential);
    }

    public Point getCentralEvent() {
        double minDistance = Double.MAX_VALUE;
        Event centralEvent = new Event();

        for (Event ce : events) {
            double curDistance = 0d;
            for (Event de : events) {
                double dij = 0d;
                if (ce.oid == de.oid) {
                    dij = ce.potential;
                } else {
                    switch (distanceMethod) {
                    case Euclidean:
                        dij = SSUtils.getEuclideanDistance(ce.x, ce.y, de.x, de.y);
                        break;
                    case Manhattan:
                        dij = SSUtils.getManhattanDistance(ce.x, ce.y, de.x, de.y);
                        break;
                    }
                }
                curDistance += (dij * de.weight);
            }

            if (minDistance > curDistance) {
                minDistance = curDistance;
                centralEvent = ce;
            }
        }

        return gf.createPoint(new Coordinate(centralEvent.x, centralEvent.y));
    }

    public DistanceMethod getDistanceMethod() {
        return distanceMethod;
    }

    public void setDistanceMethod(DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;
    }
}