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

import org.geotools.process.spatialstatistics.enumeration.StaticsType;

/**
 * StatisticsField
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsField {
    StaticsType statType = StaticsType.Count;

    private String srcField = null;

    private String targetField = null;

    private int fieldLength = 0;

    public Class<?> fieldType = String.class;

    public double tagValue = 0;

    public double tagValue2 = 0;

    public Object value = null;

    public StatisticsField() {
    };

    public StatisticsField(String srcField, String targetField) {
        setSrcField(srcField);
        setTargetField(targetField);
    }

    public void setSrcField(String srcField) {
        this.srcField = srcField;
    }

    public String getSrcField() {
        return srcField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setFieldLength(int fieldLength) {
        this.fieldLength = fieldLength;
    }

    public int getFieldLength() {
        return fieldLength;
    }

    public void setStatType(StaticsType statType) {
        this.statType = statType;
    }

    public StaticsType getStatType() {
        return statType;
    }
}
