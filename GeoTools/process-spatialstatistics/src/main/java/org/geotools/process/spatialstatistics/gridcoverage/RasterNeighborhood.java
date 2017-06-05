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
package org.geotools.process.spatialstatistics.gridcoverage;

import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Raster Neighborhood
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterNeighborhood {
    
    public enum NeighborUnits {
        CELL, MAP
    }

    public enum NeighborhoodType {
        CIRCLE, RECTANGLE
    }

    public RasterNeighborhood() {
        setDefault();
    }

    public RasterNeighborhood(double radius, NeighborUnits unitsType) {
        setCircle(radius, unitsType);
    }

    public RasterNeighborhood(double width, double height, NeighborUnits unitsType) {
        setRectangle(width, height, unitsType);
    }

    NeighborUnits neighborUnits = NeighborUnits.CELL;

    public NeighborUnits getNeighborUnits() {
        return neighborUnits;
    }

    private NeighborhoodType neighborhoodType = NeighborhoodType.RECTANGLE;

    public NeighborhoodType getNeighborType() {
        return neighborhoodType;
    }

    private double radius = 0.0;

    public double getRadius() {
        return this.radius;
    }

    public void setCircle(double radius, NeighborUnits unitsType) {
        neighborhoodType = NeighborhoodType.CIRCLE;
        this.radius = radius;
        neighborUnits = unitsType;
    }

    private double width = 3;

    public double getWidth() {
        return this.width;
    }

    private double height = 3;

    public double getHeight() {
        return this.height;
    }

    public void setRectangle(double width, double height, NeighborUnits unitsType) {
        this.neighborhoodType = NeighborhoodType.RECTANGLE;
        this.width = width;
        this.height = height;
        this.neighborUnits = unitsType;
    }

    public void setDefault() {
        setRectangle(3, 3, NeighborUnits.CELL);
    }

    public static RasterNeighborhood getDefaultNeighbor(ReferencedEnvelope srcExtent,
            NeighborhoodType nhType) {
        RasterNeighborhood nh = new RasterNeighborhood();

        if (nhType == NeighborhoodType.CIRCLE) {
            // The cell size is the shortest of the width or height of the extent of
            // in_point_features in the output spatial reference, divided by 30
            double rd = Math.min(srcExtent.getWidth(), srcExtent.getHeight()) / 30.0;
            nh.setCircle(rd, NeighborUnits.MAP);
        } else {
            nh.setDefault();
        }

        return nh;
    }
}