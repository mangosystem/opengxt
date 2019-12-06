/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
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

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * Value-Count Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ValueCountVisitor {
    protected static final Logger LOGGER = Logging.getLogger(ValueCountVisitor.class);

    private SortedMap<Object, Integer> valueCountsMap;

    private Object[] arrayValues;

    private int[] arrayFrequencies;

    public ValueCountVisitor() {
        valueCountsMap = new TreeMap<Object, Integer>();
    }

    public Object[] getArrayValues() {
        if (arrayValues == null) {
            buildArrays();
        }
        return this.arrayValues;
    }

    public int[] getArrayFrequencies() {
        if (arrayFrequencies == null) {
            buildArrays();
        }
        return this.arrayFrequencies;
    }

    private void buildArrays() {
        arrayValues = new Object[valueCountsMap.size()];
        arrayFrequencies = new int[valueCountsMap.size()];

        int k = 0;
        for (Entry<Object, Integer> entrySet : valueCountsMap.entrySet()) {
            arrayValues[k] = entrySet.getKey();
            arrayFrequencies[k] = entrySet.getValue();
            k++;
        }
    }

    public void visit(Object sampleValue) {
        if (valueCountsMap.containsKey(sampleValue)) {
            final int cnt = valueCountsMap.get(sampleValue);
            valueCountsMap.put(sampleValue, Integer.valueOf(cnt + 1));
        } else {
            valueCountsMap.put(sampleValue, Integer.valueOf(1));
        }
    }

}
