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
     * Nearby neighboring features have a larger influence on the computations for a target feature than features that are far away. distance decay =
     * 1.
     */
    InverseDistance,

    /**
     * Same as InverseDistance except that the slope is sharper, so influence drops off more quickly, and only a target feature's closest neighbors
     * will exert substantial influence on computations for that feature. distance decay = 2.
     */
    InverseDistanceSquared,

    /**
     * Each feature is analyzed within the context of neighboring features. Neighboring features inside the specified critical distance receive a
     * weight of 1 and exert influence on computations for the target feature.
     */
    FixedDistance,

    /**
     * Features within the specified critical distance of a target feature receive a weight of 1 and influence computations for that feature.
     */
    ZoneOfIndifference,

    /**
     * K Nearest Neighbors (KNN) is a distance-based definition of neighbors where "k" refers to the number of neighbors of a location. It is computed
     * as the distance between a point and the number (k) of nearest neighbor points (i.e. the distance between the central points of polygons).
     */
    KNearestNeighbors,

    /**
     * Polygon features that share a boundary, share a node, or overlap will influence computations for the target polygon feature.
     */
    ContiguityEdgesNodes,

    /**
     * Only neighboring polygon features that share a boundary or overlap will influence computations for the target polygon feature.
     */
    ContiguityEdgesOnly,

    /**
     * Only neighboring polygon features that share a node will influence computations for the target polygon feature.
     */
    ContiguityNodesOnly,

    /**
     * Spatial relationships are defined by a specified spatial weights file.
     */
    WeightsFromFile
}
