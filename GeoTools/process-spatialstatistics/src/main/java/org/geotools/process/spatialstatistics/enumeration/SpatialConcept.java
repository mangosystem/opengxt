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
 * SpatialConcept
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum SpatialConcept {
    /**
     * Nearby neighboring features have a larger influence on the computations for a target
     * feature than features that are far away.
     */
    INVERSEDISTANCE,

    /**
     * Same as INVERSE_DISTANCE except that the slope is sharper, so influence drops off more
     * quickly, and only a target feature's closest neighbors will exert substantial influence
     * on computations for that feature.
     */
    INVERSEDISTANCESQUARED,

    /**
     * Each feature is analyzed within the context of neighboring features. Neighboring features
     * inside the specified critical distance receive a weight of 1 and exert influence on
     * computations for the target feature.
     */
    FIXEDDISTANCEBAND,

    /**
     * Features within the specified critical distance of a target feature receive a weight of 1
     * and influence computations for that feature.
     */
    ZONEOFINDIFFERENCE,

    /**
     * Polygon features that share a boundary, share a node, or overlap will influence
     * computations for the target polygon feature.
     */
    POLYGONCONTIGUITY,

    /**
     * Spatial weights from file
     */
    SPATIALWEIGHTSFROMFILE

}
