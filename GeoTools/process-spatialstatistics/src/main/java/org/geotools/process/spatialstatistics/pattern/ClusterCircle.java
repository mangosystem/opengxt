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
package org.geotools.process.spatialstatistics.pattern;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Cluster Circle
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @see https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public class ClusterCircle {
    protected static final Logger LOGGER = Logging.getLogger(ClusterCircle.class);

    static final GeometryFactory gf = new GeometryFactory();

    private Point center;

    private Envelope extent;

    private Geometry circle;

    private double radius;

    private double fitness = 0.0;

    private double population = 0.0;

    private double expected = 0.0;

    private double cases = 0.0;

    public ClusterCircle(double x, double y, double radius) {
        this.center = gf.createPoint(new Coordinate(x, y));
        this.radius = radius;
        this.extent = new Envelope(x - radius, x + radius, y - radius, y + radius);
        this.circle = center.buffer(radius, 24);
    }

    public Point getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public double getX() {
        return center.getX();
    }

    public double getY() {
        return center.getY();
    }

    public boolean contains(Point p) {
        if (center == null) {
            return false;
        }
        return center.distance(p) <= radius;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getPopulation() {
        return population;
    }

    public void setPopulation(double population) {
        this.population = population;
    }

    public double getExpected() {
        return expected;
    }

    public void setExpected(double expected) {
        this.expected = expected;
    }

    public double getCases() {
        return cases;
    }

    public void setCases(double cases) {
        this.cases = cases;
    }

    public Geometry getPolygon() {
        return circle;
    }

    public Envelope getBounds() {
        return extent;
    }

    @Override
    public String toString() {
        return center + " " + radius;
    }
}