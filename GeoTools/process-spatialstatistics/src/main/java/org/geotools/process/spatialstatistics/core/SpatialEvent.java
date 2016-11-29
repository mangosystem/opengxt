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

import com.vividsolutions.jts.geom.Coordinate;

/**
 * SpatialEvent
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialEvent {

    public Object oid = 0;

    public double x = Double.NaN;

    public double y = Double.NaN;

    public double weight = 1.0;

    public Object tagValue = null;

    public boolean hasNeighbor = true;

    public double population = 1.0;

    public SpatialEvent(int objectID) {
        this.oid = objectID;
    }

    public SpatialEvent(Object objectID, Coordinate coordinate) {
        this.oid = objectID;
        this.x = coordinate.x;
        this.y = coordinate.y;
    }

    public SpatialEvent(Object objectID, Coordinate coordinate, double obsValue) {
        this(objectID, coordinate);
        this.weight = obsValue;
    }

    public SpatialEvent(Object objectID, Coordinate coordinate, double obsValue, double popValue) {
        this(objectID, coordinate, obsValue);
        this.population = popValue;
    }

    public double getDistance(SpatialEvent other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public double getDistance(Coordinate other) {
        return other.distance(new Coordinate(this.x, this.y));
    }

    public Coordinate getCoordinate() {
        return new Coordinate(x, y);
    }
}
