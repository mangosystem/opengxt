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
 * Point Location Type
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum PointLocationType {
    /**
     * A point will be created at each input feature vertex. This is the default.
     */
    All,

    /**
     * A point will be created at the midpoint, not necessarily a vertex, of each input line or
     * polygon boundary.
     */
    Mid,

    /**
     * A point will be created at the start point (first vertex) of each input feature.
     */
    Start,

    /**
     * A point will be created at the end point (last vertex) of each input feature.
     */
    End,

    /**
     * Two points will be created, one at the start point and another at the endpoint of each input
     * feature.
     */
    BothEnds
}
