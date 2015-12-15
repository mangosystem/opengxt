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
 * Contiguity-Based Spatial Weights Types
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum ContiguityType {
    /**
     * Polygon Contiguity (Edges and Corners)—A queen weights matrix defines a location's neighbors as those with either a shared border or vertex
     */
    Queen,

    /**
     * Polygon Contiguity (Edges Only)—A rook weights matrix defines a location's neighbors as those areas with shared borders
     */
    Rook,

    /**
     * Polygon Contiguity (Corners Only)—A rook weights matrix defines a location's neighbors as those areas with shared vertex
     */
    Bishops
}
