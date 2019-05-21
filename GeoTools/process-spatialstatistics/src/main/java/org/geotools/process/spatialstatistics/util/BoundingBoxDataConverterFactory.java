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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * ConverterFactory for trading between strings and ReferenceEnvelope
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BoundingBoxDataConverterFactory implements ConverterFactory {

    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if (target.equals(ReferencedEnvelope.class) && source.equals(String.class)) {
            return new Converter() {

                @SuppressWarnings("unchecked")
                public <T> T convert(Object source, Class<T> target) throws Exception {
                    String[] coordinates = ((String) source).split(",");
                    if (coordinates.length < 4) {
                        return null;
                    }

                    double x1 = Double.parseDouble(coordinates[0]);
                    double y1 = Double.parseDouble(coordinates[1]);
                    double x2 = Double.parseDouble(coordinates[2]);
                    double y2 = Double.parseDouble(coordinates[3]);

                    CoordinateReferenceSystem crs = null;
                    if (coordinates.length > 4) {
                        crs = CRS.decode(coordinates[4]);
                    }

                    return (T) new ReferencedEnvelope(x1, x2, y1, y2, crs);
                }

            };
        }

        return null;
    }

}
