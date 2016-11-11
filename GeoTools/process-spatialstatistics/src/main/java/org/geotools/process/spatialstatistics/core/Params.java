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
package org.geotools.process.spatialstatistics.core;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Parameter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Process parameters utilities
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class Params {
    protected static final Logger LOGGER = Logging.getLogger(Params.class);

    /**
     * MetaData for FeatureCollection Parameter
     */
    public static final String FEATURES = "Features";

    /**
     * MetaData for Fields Parameter
     */
    public static final String FIELDS = "Fields";

    /**
     * MetaData for Field Parameter
     */
    public static final String FIELD = "Field";

    /**
     * MetaData for Fields Parameter
     */
    public static final String STYLES = "Styles";

    /**
     * Geometry Type for Polygon, LineString, Point, Polyline(Polygon or LineString), Multipart
     */
    public static final String Polygon = "Polygon";

    public static final String LineString = "LineString";

    public static final String Point = "Point";

    public static final String Polyline = "Polyline";

    public static final String Multipart = "Multipart";

    public static Object getValue(Map<String, Object> input, Parameter<?> parameter,
            Object defaultValue) {
        Object param = input.get(parameter.key);

        if (param == null) {
            return defaultValue; // input.get(parameter.sample);
        }

        // check type
        if (!parameter.type.isAssignableFrom(param.getClass())) {
            String msg = parameter.key + " require " + parameter.type.getSimpleName()
                    + " type!, not " + param.getClass().getSimpleName() + " type!!!";
            throw new ClassCastException(msg);
        }

        return param;
    }

    public static Object getValue(Map<String, Object> input, Parameter<?> parameter) {
        Object param = input.get(parameter.key);

        if (param == null) {
            return input.get(parameter.sample);
        }

        // check type
        if (!parameter.type.isAssignableFrom(param.getClass())) {
            String msg = parameter.key + " require " + parameter.type.getSimpleName()
                    + " type!, not " + param.getClass().getSimpleName() + " type!!!";
            throw new ClassCastException(msg);
        }

        return param;
    }

    // create boundingbox from comma separated coordinates string and crs
    public static ReferencedEnvelope getBoundingBox(String bBox, CoordinateReferenceSystem crs) {
        if (bBox == null || bBox.isEmpty()) {
            return new ReferencedEnvelope(crs);
        }

        String[] coords = bBox.split(",");

        try {
            // BBOX: XMIN, YMIN, XMAX, YMAX
            double minx = Double.parseDouble(coords[0]);
            double miny = Double.parseDouble(coords[1]);
            double maxx = Double.parseDouble(coords[2]);
            double maxy = Double.parseDouble(coords[3]);

            return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new ReferencedEnvelope(crs);
    }

}
