/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Number Format Utilities
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
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
