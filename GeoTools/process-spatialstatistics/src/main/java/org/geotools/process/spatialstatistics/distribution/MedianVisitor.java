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
package org.geotools.process.spatialstatistics.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Median Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MedianVisitor {
    @SuppressWarnings("rawtypes")
    private List list = new ArrayList();

    @SuppressWarnings("unchecked")
    public void visit(Object value) {
        list.add(value);
    }

    @SuppressWarnings("unchecked")
    public Number getMedian() {
        final int size = list.size();
        if (size < 1) {
            return null;
        } else if (size == 1) {
            return (Number) list.get(0);
        }

        Number median = null;
        Collections.sort(list);

        final int index = size / 2;
        if ((size % 2) == 0) {
            Object input1 = list.get(index - 1);
            Object input2 = list.get(index);

            if ((input1 instanceof Number) && (input2 instanceof Number)) {
                Number num1 = (Number) input1;
                Number num2 = (Number) input2;
                median = (num1.doubleValue() + num2.doubleValue()) / 2.0;
            } else {
                // NaN
                median = (Number) list.get(index);
            }
        } else {
            median = (Number) list.get(index);
        }

        return median;
    }
}