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

import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.Hints;

/**
 * ConverterFactory for trading between strings and filter expression
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExpressionConverterFactory implements ConverterFactory {

    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if (target.equals(Expression.class) && source.equals(String.class)) {
            return new Converter() {

                @SuppressWarnings("unchecked")
                public <T> T convert(Object source, Class<T> target) throws Exception {
                    return (T) ECQL.toExpression((String) source);
                }

            };
        }

        return null;
    }

}
