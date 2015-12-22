/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import org.geoserver.wps.ppio.LiteralPPIO;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.expression.Expression;

/**
 * A PPIO to generate good looking Filter Expression
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExpressionPPIO extends LiteralPPIO {

    public ExpressionPPIO() {
        super(Expression.class);
    }

    /**
     * Decodes the parameter (as a string) to its internal object implementation.
     */
    public Object decode(String value) throws Exception {
        if (value == null) {
            return null;
        }
        return ECQL.toExpression(value);
    }

    /**
     * Encodes the internal object representation of a parameter as a string.
     */
    public String encode(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        return ((Expression) value).toString();
    }

}
