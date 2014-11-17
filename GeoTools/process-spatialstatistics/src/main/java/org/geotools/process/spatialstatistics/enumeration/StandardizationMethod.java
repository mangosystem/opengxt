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
 * StandardizationMethod
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum StandardizationMethod {
    /**
     * No standardization of spatial weights is applied.
     */
    NONE,

    /**
     * Spatial weights are standardized; each weight is divided by its row sum (the sum of the
     * weights of all neighboring features).
     */
    ROW,

    /**
     * Nearby neighboring features have a larger influence on the computations for a target
     * feature than features that are far away.
     */
    GLOBAL

}
