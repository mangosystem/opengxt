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
 * StaticsType
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum StaticsType {
    /**
     * Finds the first record in the features and uses its specified field value.
     */
    First,

    /**
     * Finds the last record in the features and uses its specified field value.
     */
    Last,

    /**
     * Adds the total value for the specified field.
     */
    Sum,

    /**
     * Calculates the average for the specified field.
     */
    Mean,

    /**
     * Finds the smallest value for all records of the specified field.
     */
    Minimum,

    /**
     * Finds the largest value for all records of the specified field.
     */
    Maximum,

    /**
     * Finds the range of values (Maximum – Minimum) for the specified field.
     */
    Range,

    /**
     * Finds the standard deviation on values in the specified field.
     */
    StandardDeviation,

    /**
     * Finds the varience on values in the specified field.
     */
    Variance,

    /**
     * Finds the number of values included in statistical calculations.
     */
    Count
}
