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

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Histogram Features
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HistogramFeatures extends DataHistogram {
    protected static final Logger LOGGER = Logging.getLogger(HistogramFeatures.class);

    SimpleFeatureCollection features = null;

    Filter filter = Filter.INCLUDE;

    String propertyName = null;

    @Override
    public boolean calculateHistogram(SimpleFeatureSource featureSource, Filter filter,
            String attributeName) {
        if (!StringHelper.isNullOrEmpty(attributeName)) {
            attributeName = FeatureTypes.validateProperty(featureSource.getSchema(), attributeName);
        }

        this.propertyName = attributeName;
        this.filter = filter == null ? Filter.INCLUDE : filter;
        try {
            String typeName = featureSource.getSchema().getTypeName();
            Query query = new Query(typeName, filter, new String[] { attributeName });

            this.features = featureSource.getFeatures(query);
            return calculate();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean calculateHistogram(SimpleFeatureCollection features, String attributeName) {
        if (!StringHelper.isNullOrEmpty(attributeName)) {
            attributeName = FeatureTypes.validateProperty(features.getSchema(), attributeName);
        }

        this.features = features;
        this.propertyName = attributeName;

        return calculate();
    }

    private boolean calculate() {
        SortedMap<Double, Integer> valueCountsMap = new TreeMap<Double, Integer>();

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        final Expression attrExpr = ff.property(propertyName);

        count = 0;
        sumOfVals = 0;

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();

                Double value = attrExpr.evaluate(feature, Double.class);
                if (value != null) {
                    double doubleVal = value.doubleValue();
                    if (Double.isNaN(doubleVal) || Double.isInfinite(doubleVal)) {
                        continue;
                    }

                    if (valueCountsMap.containsKey(value)) {
                        final int cnt = valueCountsMap.get(value);
                        valueCountsMap.put(value, Integer.valueOf(cnt + 1));
                    } else {
                        valueCountsMap.put(value, Integer.valueOf(1));
                    }

                    count++;
                    sumOfVals += doubleVal;
                }
            }
        } finally {
            featureIter.close();
        }

        if (valueCountsMap.size() == 0) {
            return false;
        }

        doubleArrayValues = new double[valueCountsMap.size()];
        longArrayFrequencies = new int[valueCountsMap.size()];

        Iterator<Double> iterator = valueCountsMap.keySet().iterator();
        int k = 0;
        while (iterator.hasNext()) {
            final Double key = iterator.next();
            doubleArrayValues[k] = key;
            longArrayFrequencies[k] = valueCountsMap.get(key);

            k++;
        }

        return true;
    }

}
