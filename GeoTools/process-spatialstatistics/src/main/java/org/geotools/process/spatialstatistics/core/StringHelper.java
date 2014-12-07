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

import java.lang.Character.UnicodeBlock;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.util.logging.Logging;

/**
 * String Helper
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StringHelper {
    protected static final Logger LOGGER = Logging.getLogger(StringHelper.class);

    public static String removeSpecialCharacters(String fullAddress) {
        String cleanedAddr = fullAddress;

        // remove special character
        String pattern = "[~!\\@&%#$^&\\*=+|:;?\"<,.>']";
        cleanedAddr = fullAddress.replaceAll(pattern, "");

        // finally remove duplicate space
        while (cleanedAddr.contains("  ")) {
            cleanedAddr = cleanedAddr.replace("  ", " ");
        }

        return cleanedAddr;
    }

    public static String join(Object[] strings, String separator) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            if (i != 0) {
                sb.append(separator);
            }
            sb.append(strings[i]);
        }
        return sb.toString();
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNumeric(String valueCheck) {
        if (isNullOrEmpty(valueCheck)) {
            return false;
        }

        return valueCheck.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
    }

    public static boolean isDigit(String valueCheck) {
        if (isNullOrEmpty(valueCheck)) {
            return false;
        }

        for (int i = 0; i < valueCheck.length(); i++) {
            char curChar = valueCheck.charAt(i);

            if (!Character.isDigit(curChar)) {
                return false;
            }
        }
        return true;
    }

    public static String removeNumericValue(String valueCheck) {
        if (isNullOrEmpty(valueCheck)) {
            return valueCheck;
        }

        String resultString = "";
        int strLen = valueCheck.length() - 1;
        for (int i = strLen; i >= 0; i--) {
            char curChar = valueCheck.charAt(i);

            if (Character.isDigit(curChar) || "-".equals(Character.toString(curChar))) {
                // skip
            } else {
                resultString = curChar + resultString;
            }
        }

        return resultString.trim();
    }

    public static String before(String valueCheck, String splitter) {
        if (isNullOrEmpty(valueCheck)) {
            return valueCheck;
        }

        int pos = valueCheck.indexOf(splitter);
        if (pos != -1) {
            return valueCheck.substring(0, pos).trim();
        } else {
            return valueCheck;
        }
    }

    public static String after(String valueCheck, String splitter) {
        if (isNullOrEmpty(valueCheck)) {
            return valueCheck;
        }

        int pos = valueCheck.indexOf(splitter);
        if (pos != -1) {
            pos++;
            return valueCheck.substring(pos, valueCheck.length() - pos).trim();
        } else {
            return valueCheck;
        }
    }

    public static String extractNumber(String valueCheck) {
        if (isNullOrEmpty(valueCheck)) {
            return valueCheck;
        }

        String numValues = "";
        String regEx = "\\d";

        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(valueCheck);

        while (matcher.find()) {
            numValues += matcher.group(0);
        }

        return numValues;
    }

    public static boolean containsHangul(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);
            if (UnicodeBlock.HANGUL_SYLLABLES.equals(unicodeBlock)
                    || UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(unicodeBlock)
                    || UnicodeBlock.HANGUL_JAMO.equals(unicodeBlock))
                return true;
        }
        return false;
    }

    public static String substringBytes(String value, int byte_len) {
        int retLength = 0;
        int tempSize = 0;
        for (int index = 1; index <= value.length(); index++) {
            int asc = value.charAt(index - 1);
            if (asc > 127) {
                if (byte_len >= tempSize + 2) {
                    tempSize += 2;
                    retLength++;
                } else {
                    return value.substring(0, retLength);
                }
            } else {
                if (byte_len > tempSize) {
                    tempSize++;
                    retLength++;
                }
            }
        }
        return value.substring(0, retLength);
    }
}
