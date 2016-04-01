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

import java.util.ArrayList;
import java.util.List;

/**
 * Histogram Process Result.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HistogramProcessResult {

    String typeName;

    String propertyName;

    String area = Double.toString(0d);
    
    String cellSize = Double.toString(0d);

    List<HistogramItem> histogramItems;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getCellSize() {
        return cellSize;
    }

    public void setCellSize(String cellSize) {
        this.cellSize = cellSize;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public List<HistogramItem> getHistogramItems() {
        return histogramItems;
    }

    public void setHistogramItems(List<HistogramItem> histogramItems) {
        this.histogramItems = histogramItems;
    }

    public HistogramProcessResult(String typeName, String propertyName) {
        this.typeName = typeName;
        this.propertyName = propertyName;
    }

    public void putValues(Object[] arrayValues, int[] arrayFrequencies) {
        histogramItems = new ArrayList<HistogramItem>();

        for (int k = 0; k < arrayValues.length; k++) {
            histogramItems.add(new HistogramItem(arrayValues[k].toString(), arrayFrequencies[k]));
        }
    }

    public void putValues(double[] arrayValues, int[] arrayFrequencies) {
        histogramItems = new ArrayList<HistogramItem>();

        for (int k = 0; k < arrayValues.length; k++) {
            histogramItems.add(new HistogramItem(String.valueOf(arrayValues[k]),
                    arrayFrequencies[k]));
        }
    }

    @Override
    public String toString() {
        final String separator = System.getProperty("line.separator");

        StringBuffer sb = new StringBuffer();
        sb.append("TypeName").append(typeName).append(separator);
        sb.append("PropertyName").append(propertyName).append(separator);
        sb.append("Area").append(area).append(separator);
        for (int k = 0; k < histogramItems.size(); k++) {
            sb.append(histogramItems.get(k).toString()).append(separator);
        }

        return sb.toString();
    }

    public static class HistogramItem {

        String value;

        Integer frequency;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Integer getFrequency() {
            return frequency;
        }

        public void setFrequency(Integer frequency) {
            this.frequency = frequency;
        }

        public HistogramItem() {
        }

        public HistogramItem(String value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append(" Value = ").append(value).append(separator);
            sb.append(" Frequency").append(frequency).append(separator);

            return sb.toString();
        }
    }
}