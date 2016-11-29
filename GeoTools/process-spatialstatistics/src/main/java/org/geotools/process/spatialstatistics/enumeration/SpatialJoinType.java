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
 * SpatialJoinType
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum SpatialJoinType {
    /**
     * All target features will be maintained in the output (outer join). This is the default.
     */
    KeepAllRecord,

    /**
     * Only those target features that have the specified spatial relationship with the join features will be maintained in the output feature class
     * (inner join).
     */
    OnlyMatchingRecord
}
