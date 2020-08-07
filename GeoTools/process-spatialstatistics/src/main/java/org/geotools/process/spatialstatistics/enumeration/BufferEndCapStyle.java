/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
 * Buffer Cap Style
 * 
 * @reference org.locationtech.jts.operation.buffer.BufferParameters
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public enum BufferEndCapStyle {
    /**
     * Round line buffer end cap style. CAP_ROUND = 1
     */
    Round(1),

    /**
     * Flat line buffer end cap style. CAP_FLAT = 2
     */
    Flat(2),

    /**
     * Square line buffer end cap style. CAP_SQUARE = 3
     */
    Square(3);

    private final int value;

    BufferEndCapStyle(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
