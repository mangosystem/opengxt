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
 * Raster Surface Slope Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum SlopeType {
    /**
     * The inclination of slope will be calculated in degrees.
     */
    DEGREE,

    /**
     * Keyword to output the percent rise, also referred to as the percent slope.
     */
    PERCENTRISE
}
