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
 * Raster Pixel Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum RasterPixelType {
    /**
     * bytes.
     */
    BYTE,

    /**
     * 16 bits integers.
     */
    SHORT,

    /**
     * 32 bits integers.
     */
    INTEGER,

    /**
     * Simple precision floating point numbers.
     */
    FLOAT,

    /**
     * Double precision floating point numbers.
     */
    DOUBLE;
}
