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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * FormatUtils
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FormatUtils {

    static final int DEFAULT_NUMBER_DIGIT = 6;

    static final DecimalFormat df = new DecimalFormat("#.######");

    static final NumberFormat nf = NumberFormat.getInstance();

    public static double round(double val) {
        return round(val, DEFAULT_NUMBER_DIGIT);
    }

    public static double round(double val, int numberDigit) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return val;
        }

        BigDecimal bigDecimal = new BigDecimal(val);
        bigDecimal = bigDecimal.setScale(numberDigit, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }

    public static String format(double val) {
        return format(val, DEFAULT_NUMBER_DIGIT);
    }

    public static String format(double val, int numberDigit) {
        if (Double.isNaN(val)) {
            return "";
        }

        if (Double.isInfinite(val)) {
            return "";
        }

        df.setMaximumFractionDigits(numberDigit);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(val);
    }
}
