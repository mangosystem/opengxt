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
package org.geotools.process.spatialstatistics.enumeration;

/**
 * Zonal Statistics Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum ZonalStatisticsType {

    /**
     * Finds the number of values included in statistical calculations.
     */
    Count,

    /**
     * Adds the total value of all cells in the value raster that belong to the same zone as the output cell.
     */
    Sum,

    /**
     * Calculates the average of all cells in the value raster that belong to the same zone as the output cell.
     */
    Mean,

    /**
     * Finds the smallest value of all cells in the value raster that belong to the same zone as the output cell.
     */
    Minimum,

    /**
     * Finds the largest value of all cells in the value raster that belong to the same zone as the output cell.
     */
    Maximum,

    /**
     * Finds the range of values (Maximum – Minimum) of all cells in the value raster that belong to the same zone as the output cell.
     */
    Range,

    /**
     * Finds the standard deviation on values of all cells in the value raster that belong to the same zone as the output cell.
     */
    StdDev
}
