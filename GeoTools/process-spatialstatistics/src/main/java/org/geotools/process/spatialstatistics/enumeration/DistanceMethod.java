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
 * DistanceMethod
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum DistanceMethod {
    /**
     * The straight-line distance between two points (as the crow flies) in Euclidean space
     */
    Euclidean,

    /**
     * The distance between two points in a grid based on a strictly horizontal and/or vertical path (that is, along the grid lines)
     */
    Manhattan
}
