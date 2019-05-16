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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;

/**
 * Abstract Interpolator
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractInterpolator {
    protected static final Logger LOGGER = Logging.getLogger(AbstractInterpolator.class);

    /**
     * The observed data values
     * */
    protected Coordinate[] samples;

    /**
     * Helper constant for generating matrix dimensions
     * */
    protected int number = 0;

    /**
     * Get interpolated value
     * 
     * @param p location
     * @return interpolated value
     */
    public abstract double getValue(Coordinate p);

}
