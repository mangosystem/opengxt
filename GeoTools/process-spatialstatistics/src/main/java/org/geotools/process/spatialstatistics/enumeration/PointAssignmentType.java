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
 * The method to determine how the cell will be assigned a value when more than one feature falls within a cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum PointAssignmentType {

    /**
     * If there is more than one feature within the cell, the one with the most common attribute, in <field>, is assigned to the cell. If they
     * have the same number of common attributes, the one with the lowest FID is used.
     * 
     */
    MostFrequent,

    /**
     * The sum of the attributes of all the points within the cell (not valid for string data).
     * 
     */
    Sum,

    /**
     * The mean of the attributes of all the points within the cell (not valid for string data).
     * 
     */
    Mean,

    /**
     * The standard deviation of attributes of all the points within the cell. If there are less than two points in the cell, the cellis assigned
     * NoData (not valid for string data).
     * 
     */
    StandardDeviation,

    /**
     * The maximum value of the attributes of the points within the cell (not valid for string data).
     * 
     */
    Maximum,

    /**
     * The minimum value of the attributes of the points within the cell (not valid for string data).
     * 
     */
    Minimum,

    /**
     * The range of the attributes of the points within the cell (not valid for string data).
     * 
     */
    Range,

    /**
     * The number of points within the cell.
     * 
     */
    Count
}