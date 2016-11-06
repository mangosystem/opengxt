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
package org.geotools.process.spatialstatistics.util;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;

/**
 * Translates the geometry to a new location using the numeric parameters as offsets. <br>
 * 
 * <pre>
 * geometry.apply(new CoordinateTranslateFilter(dx, dy));
 * </pre>
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CoordinateTranslateFilter implements CoordinateSequenceFilter {
    protected final Logger LOGGER = Logging.getLogger(CoordinateTranslateFilter.class);

    private double dx = 0.0;

    private double dy = 0.0;

    public CoordinateTranslateFilter(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void filter(CoordinateSequence seq, int i) {
        seq.setOrdinate(i, 0, seq.getOrdinate(i, 0) + dx);
        seq.setOrdinate(i, 1, seq.getOrdinate(i, 1) + dy);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isGeometryChanged() {
        return true;
    }
}
