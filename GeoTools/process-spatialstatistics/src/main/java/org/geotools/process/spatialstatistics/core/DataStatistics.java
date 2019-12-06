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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * Data Statistics
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DataStatistics {
    protected static final Logger LOGGER = Logging.getLogger(DataStatistics.class);

    public ValueCountVisitor getValueCountMap(SimpleFeatureCollection features,
            String propertyName) {
        if (!StringHelper.isNullOrEmpty(propertyName)) {
            propertyName = FeatureTypes.validateProperty(features.getSchema(), propertyName);
        }

        int idxField = features.getSchema().indexOf(propertyName);
        if (idxField == -1) {
            LOGGER.warning(propertyName + " does not exist!");
            return new ValueCountVisitor();
        }

        ValueCountVisitor vcVisitor = new ValueCountVisitor();

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                vcVisitor.visit(feature.getAttribute(idxField));
            }
        } finally {
            featureIter.close();
        }

        return vcVisitor;
    }

    public Object[] getUniqueValues(SimpleFeatureSource featuresource, Filter filter,
            String propertyName) {
        if (!StringHelper.isNullOrEmpty(propertyName)) {
            propertyName = FeatureTypes.validateProperty(featuresource.getSchema(), propertyName);
        }

        try {
            filter = filter == null ? Filter.INCLUDE : filter;
            String typeName = featuresource.getSchema().getTypeName();
            Query query = new Query(typeName, filter, new String[] { propertyName });

            return getUniqueValues(featuresource.getFeatures(query), propertyName);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return null;
    }

    public Object[] getUniqueValues(SimpleFeatureCollection features, String propertyName) {
        SortedMap<String, Object> valueMap = new TreeMap<String, Object>();

        if (!StringHelper.isNullOrEmpty(propertyName)) {
            propertyName = FeatureTypes.validateProperty(features.getSchema(), propertyName);
        }

        int idxField = features.getSchema().indexOf(propertyName);
        if (idxField == -1) {
            LOGGER.warning(propertyName + " does not exist!");
            return null;
        }

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Object curVal = feature.getAttribute(idxField);
                if (curVal != null) {
                    valueMap.put(curVal.toString(), curVal);
                }
            }
        } finally {
            featureIter.close();
        }

        if (valueMap.size() == 0) {
            return null;
        }

        return valueMap.values().toArray();
    }

    public StatisticsVisitorResult getStatistics(SimpleFeatureSource featuresource, Filter filter,
            String propertyName) {
        if (!StringHelper.isNullOrEmpty(propertyName)) {
            propertyName = FeatureTypes.validateProperty(featuresource.getSchema(), propertyName);
        }

        try {
            filter = filter == null ? Filter.INCLUDE : filter;
            String typeName = featuresource.getSchema().getTypeName();
            Query query = new Query(typeName, filter, new String[] { propertyName });

            return getStatistics(featuresource.getFeatures(query), propertyName);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }

        return null;
    }

    public StatisticsVisitorResult getStatistics(SimpleFeatureCollection features,
            String propertyName) {
        if (!StringHelper.isNullOrEmpty(propertyName)) {
            propertyName = FeatureTypes.validateProperty(features.getSchema(), propertyName);
        }

        int idxField = features.getSchema().indexOf(propertyName);
        if (idxField == -1) {
            LOGGER.log(Level.FINE, propertyName + " does not exist!");
            return null;
        }

        StatisticsVisitor visitor = new StatisticsVisitor(features.getSchema(), idxField);
        visitor.visit(features);

        return visitor.getResult();
    }

    public StatisticsVisitorResult getStatistics(GridCoverage2D coverage, int bandIndex) {
        StatisticsVisitor visitor = new StatisticsVisitor(new DoubleStrategy());

        final double noDataValue = RasterHelper.getNoDataValue(coverage);
        visitor.setNoData(noDataValue);
        visitor.visit(coverage, bandIndex);

        return visitor.getResult();
    }

}
