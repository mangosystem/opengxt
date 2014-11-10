package org.geotools.process.spatialstatistics.core;

import com.vividsolutions.jts.geom.Coordinate;

public class SpatialEvent {

    public int oid = 0;

    public double x = Double.NaN;

    public double y = Double.NaN;

    public double weight = 1.0;

    public Object tagValue = null;

    public boolean hasNeighbor = true;

    public double population = 1.0;

    public SpatialEvent(int objectID) {
        this.oid = objectID;
    }

    public SpatialEvent(int objectID, Coordinate coordinate) {
        this.oid = objectID;
        this.x = coordinate.x;
        this.y = coordinate.y;
    }

    public SpatialEvent(int objectID, Coordinate coordinate, double obsValue) {
        this(objectID, coordinate);
        this.weight = obsValue;
    }

    public SpatialEvent(int objectID, Coordinate coordinate, double obsValue, double popValue) {
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
}
