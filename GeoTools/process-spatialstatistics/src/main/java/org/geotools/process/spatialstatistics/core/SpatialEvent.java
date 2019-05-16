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

import org.locationtech.jts.geom.Coordinate;

/**
 * SpatialEvent
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialEvent {

    public Object id = 0;

    public Coordinate coordinate;

    public double xVal = 1.0;

    public double yVal = 1.0;

    public Object tagVal = null;

    public SpatialEvent(Object id) {
        this.id = id;
    }

    public SpatialEvent(Object id, Coordinate coordinate) {
        this.id = id;
        this.coordinate = coordinate;
    }

    public SpatialEvent(Object id, Coordinate coordinate, double xVal) {
        this(id, coordinate);
        this.xVal = xVal;
    }

    public SpatialEvent(Object id, Coordinate coordinate, double xVal, double yVal) {
        this(id, coordinate, xVal);
        this.yVal = yVal;
    }

    public double distance(SpatialEvent other) {
        return this.coordinate.distance(other.getCoordinate());
    }

    public Coordinate getCoordinate() {
        return this.coordinate;
    }
}
