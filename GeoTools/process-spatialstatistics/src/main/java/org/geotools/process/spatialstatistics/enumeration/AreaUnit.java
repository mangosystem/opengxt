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
 * Area Unit
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum AreaUnit {
    /**
     * If the units are not specified, or Default is used, the linear unit of the input features' spatial reference is used.
     */
    Default,
    /**
     * SquareMeters
     */
    SquareMeters,
    /**
     * SquareKilometers
     */
    SquareKilometers,
    /**
     * SquareFeet
     */
    SquareFeet,
    /**
     * SquareYards
     */
    SquareYards,
    /**
     * SquareMiles
     */
    SquareMiles,
    /**
     * Hectare
     */
    Hectare,
    /**
     * Acre
     */
    Acre
}
