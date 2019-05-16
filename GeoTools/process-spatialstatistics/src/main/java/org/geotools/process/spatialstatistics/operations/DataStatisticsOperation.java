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
package org.geotools.process.spatialstatistics.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.gridcoverage.RasterCropOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult.DataStatisticsItem;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * DataStatisticsOperation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DataStatisticsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(DataStatisticsOperation.class);

    public DataStatisticsResult execute(GridCoverage2D inputCoverage, Geometry cropShape,
            Integer bandIndex) {
        DataStatisticsResult result = new DataStatisticsResult();

        int bandCount = inputCoverage.getNumSampleDimensions();
        if (bandIndex >= bandCount) {
            throw new ArrayIndexOutOfBoundsException("Process failed during execution");
        }

        String typeName = inputCoverage.getName().toString();
        String propertyName = "Value";
        Double noData = RasterHelper.getNoDataValue(inputCoverage);

        StatisticsVisitor visitor = new StatisticsVisitor(new DoubleStrategy());
        visitor.setNoData(noData);

        if (cropShape == null) {
            visitor.visit(inputCoverage, bandIndex);
        } else {
            RasterCropOperation cropOp = new RasterCropOperation();
            visitor.visit(cropOp.execute(inputCoverage, cropShape), bandIndex);
        }
        StatisticsVisitorResult ret = visitor.getResult();

        // remap for WPS PPIO
        DataStatisticsItem item = new DataStatisticsItem(typeName, propertyName);
        item.setCount(ret.getCount());
        item.setInvalidCount(ret.getInvalidCount());

        item.setSum(ret.getSum());
        item.setMinimum(ret.getMinimum());
        item.setMaximum(ret.getMaximum());
        item.setMean(ret.getMean());
        item.setStandardDeviation(ret.getStandardDeviation());
        item.setVariance(ret.getVariance());
        item.setCoefficientOfVariance(ret.getCoefficientOfVariance());

        item.setRange(ret.getRange());
        item.setRanges(ret.getMinimum() + " - " + ret.getMaximum());

        item.setNoData(ret.getNoData());

        result.add(item);

        return result;
    }

    public DataStatisticsResult execute(SimpleFeatureCollection inputFeatures, String fieldNames,
            String caseField) {
        if (caseField == null || caseField.length() == 0) {
            return execute(inputFeatures, fieldNames);
        }

        DataStatisticsResult result = new DataStatisticsResult();

        SimpleFeatureType featureType = inputFeatures.getSchema();
        String typeName = featureType.getTypeName();
        caseField = FeatureTypes.validateProperty(featureType, caseField);
        if (featureType.indexOf(caseField) == -1) {
            throw new NullPointerException(caseField + " does not exist!");
        }

        Expression expression = ff.property(caseField);

        String[] fields = fieldNames.split(",");
        for (int idx = 0; idx < fields.length; idx++) {
            String propertyName = fields[idx].trim();
            propertyName = FeatureTypes.validateProperty(featureType, propertyName);
            if (featureType.indexOf(propertyName) == -1) {
                LOGGER.log(Level.FINER, propertyName + " does not exist!");
                continue;
            }

            // calculate
            SimpleFeatureIterator featureIter = inputFeatures.features();
            try {
                Hashtable<String, StatisticsVisitor> map = new Hashtable<String, StatisticsVisitor>();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Object caseValue = expression.evaluate(feature);
                    caseValue = caseValue == null ? "Null" : caseValue.toString();

                    StatisticsVisitor visitor = map.get(caseValue);
                    if (visitor == null) {
                        visitor = new StatisticsVisitor(featureType, propertyName);
                        map.put((String) caseValue, visitor);
                    }
                    visitor.visit(feature);
                }

                // sort by key
                List<String> caseKeys = Collections.list(map.keys());
                Collections.sort(caseKeys);

                // add result
                Iterator<String> keyIter = caseKeys.iterator();
                while (keyIter.hasNext()) {
                    String caseValue = keyIter.next();
                    DataStatisticsItem item = remap(map.get(caseValue).getResult(), typeName,
                            propertyName, caseValue);
                    result.add(item);
                }
            } finally {
                featureIter.close();
            }
        }
        return result;
    }

    public DataStatisticsResult execute(SimpleFeatureCollection inputFeatures, String fieldNames) {
        DataStatisticsResult result = new DataStatisticsResult();

        SimpleFeatureType featureType = inputFeatures.getSchema();
        String typeName = featureType.getTypeName();

        String[] fields = fieldNames.split(",");
        for (int idx = 0; idx < fields.length; idx++) {
            String propertyName = fields[idx].trim();
            propertyName = FeatureTypes.validateProperty(featureType, propertyName);
            if (featureType.indexOf(propertyName) == -1) {
                LOGGER.log(Level.FINER, propertyName + " does not exist!");
                continue;
            }

            // calculate
            StatisticsVisitor visitor = new StatisticsVisitor(featureType, propertyName);
            visitor.visit(inputFeatures);

            // remap for WPS PPIO
            DataStatisticsItem item = remap(visitor.getResult(), typeName, propertyName, null);

            result.add(item);
        }

        return result;
    }

    private DataStatisticsItem remap(StatisticsVisitorResult ret, String typeName,
            String propertyName, String caseValue) {
        DataStatisticsItem item = new DataStatisticsItem(typeName, propertyName, caseValue);
        item.setCount(ret.getCount());
        item.setInvalidCount(ret.getInvalidCount());

        item.setSum(ret.getSum());
        item.setMinimum(ret.getMinimum());
        item.setMaximum(ret.getMaximum());
        item.setMean(ret.getMean());
        item.setStandardDeviation(ret.getStandardDeviation());
        item.setVariance(ret.getVariance());
        item.setCoefficientOfVariance(ret.getCoefficientOfVariance());

        item.setRange(ret.getRange());
        item.setRanges(ret.getMinimum() + " - " + ret.getMaximum());

        item.setNoData(ret.getNoData());

        return item;
    }

    // WPS PPIO output XML for Statistics Process Result
    public static class DataStatisticsResult {

        @XStreamImplicit(itemFieldName = "Item")
        List<DataStatisticsItem> list = new ArrayList<DataStatisticsItem>();

        public List<DataStatisticsItem> getList() {
            return this.list;
        }

        public void add(DataStatisticsItem item) {
            this.list.add(item);
        }

        public void setList(List<DataStatisticsItem> list) {
            this.list = list;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            for (DataStatisticsItem ex : list) {
                sb.append(ex.toString()).append(separator);
            }
            return sb.toString().trim();
        }

        // WPS PPIO output XML for Statistics Process Result
        public static class DataStatisticsItem {
            String typeName;

            String caseValue;

            String propertyName;

            Integer count;

            Integer invalidCount;

            Double minimum;

            Double maximum;

            Double range;

            String ranges;

            Double sum;

            Double mean;

            Double variance;

            Double standardDeviation;

            Double coefficientOfVariance;

            Object noData;

            public DataStatisticsItem(String typeName, String propertyName) {
                this.typeName = typeName;
                this.propertyName = propertyName;
            }

            public DataStatisticsItem(String typeName, String propertyName, String caseValue) {
                this.typeName = typeName;
                this.propertyName = propertyName;
                this.caseValue = caseValue;
            }

            public DataStatisticsItem(String typeName, String propertyName, int count, double sum,
                    double min, double max, double mean, double standardDeviation, double variance,
                    double range, double coefficientOfVariance) {
                this.typeName = typeName;
                this.propertyName = propertyName;

                this.count = count;
                this.sum = sum;
                this.minimum = min;
                this.maximum = max;
                this.mean = mean;
                this.standardDeviation = standardDeviation;
                this.variance = variance;
                this.range = range;
                this.coefficientOfVariance = coefficientOfVariance;
            }

            public String getTypeName() {
                return typeName;
            }

            public void setTypeName(String typeName) {
                this.typeName = typeName;
            }

            public String getCaseValue() {
                return caseValue;
            }

            public void setCaseValue(String caseValue) {
                this.caseValue = caseValue;
            }

            public String getPropertyName() {
                return propertyName;
            }

            public void setPropertyName(String propertyName) {
                this.propertyName = propertyName;
            }

            public String getRanges() {
                return ranges;
            }

            public void setRanges(String ranges) {
                this.ranges = ranges;
            }

            public Integer getCount() {
                return count;
            }

            public void setCount(Integer count) {
                this.count = count;
            }

            public int getInvalidCount() {
                return invalidCount;
            }

            public void setInvalidCount(int invalidCount) {
                this.invalidCount = invalidCount;
            }

            public Double getSum() {
                return sum;
            }

            public void setSum(Double sum) {
                this.sum = sum;
            }

            public Double getMinimum() {
                return minimum;
            }

            public void setMinimum(Double min) {
                this.minimum = min;
            }

            public Double getMaximum() {
                return maximum;
            }

            public void setMaximum(Double max) {
                this.maximum = max;
            }

            public Double getMean() {
                return mean;
            }

            public void setMean(Double mean) {
                this.mean = mean;
            }

            public Double getStandardDeviation() {
                return standardDeviation;
            }

            public void setStandardDeviation(Double standardDeviation) {
                this.standardDeviation = standardDeviation;
            }

            public Double getVariance() {
                return variance;
            }

            public void setVariance(Double variance) {
                this.variance = variance;
            }

            public Double getRange() {
                return range;
            }

            public void setRange(Double range) {
                this.range = range;
            }

            public Double getCoefficientOfVariance() {
                return coefficientOfVariance;
            }

            public void setCoefficientOfVariance(Double coefficientOfVariance) {
                this.coefficientOfVariance = coefficientOfVariance;
            }

            public Object getNoData() {
                return noData;
            }

            public void setNoData(Object noData) {
                this.noData = noData;
            }

            @Override
            public String toString() {
                final String separator = System.getProperty("line.separator");

                StringBuffer sb = new StringBuffer();
                sb.append("TypeName: ").append(typeName).append(separator);
                sb.append("PropertyName: ").append(propertyName).append(separator);
                sb.append("Count: ").append(count).append(separator);
                if (invalidCount != null) {
                    sb.append("Invalid Count: ").append(invalidCount).append(separator);
                }
                sb.append("Min: ").append(minimum).append(separator);
                sb.append("Max: ").append(maximum).append(separator);
                sb.append("Range: ").append(range).append(separator);
                sb.append("Ranges: ").append(ranges).append(separator);
                sb.append("Sum: ").append(sum).append(separator);
                sb.append("Mean: ").append(mean).append(separator);
                sb.append("Variance: ").append(variance).append(separator);
                sb.append("StandardDeviation: ").append(standardDeviation).append(separator);
                sb.append("CoefficientOfVariance: ").append(coefficientOfVariance);
                if (noData != null) {
                    sb.append(separator).append("NoData: ").append(noData);
                }

                return sb.toString();
            }
        }

    }

}
