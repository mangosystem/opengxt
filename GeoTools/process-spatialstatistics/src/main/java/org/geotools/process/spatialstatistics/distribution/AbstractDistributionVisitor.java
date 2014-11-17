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
package org.geotools.process.spatialstatistics.distribution;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Abstract Distribution Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractDistributionVisitor {
    protected static final String CASE_ALL = "ALL";

    @SuppressWarnings("rawtypes")
    protected HashMap resuleMap = new LinkedHashMap();

    @SuppressWarnings("rawtypes")
    public HashMap getResult() {
        return resuleMap;
    }

}
