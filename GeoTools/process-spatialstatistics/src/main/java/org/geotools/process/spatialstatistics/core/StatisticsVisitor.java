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

import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Statistics Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsVisitor {
    protected static final Logger LOGGER = Logging.getLogger(StatisticsVisitor.class);

    // FIRST, LAST, COUNT, SUM, MEAN, MIN, MAX, RANGE, STD, VAR, CoefficientOfVariance

    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private Expression expression;

    private StatisticsStrategy strategy = null;

    public StatisticsVisitor(SimpleFeatureType schema, String propertyName)
            throws IllegalFilterException {
        init(schema, schema.getDescriptor(FeatureTypes.validateProperty(schema, propertyName)));
    }

    public StatisticsVisitor(SimpleFeatureType schema, int attrIndex) throws IllegalFilterException {
        init(schema, schema.getDescriptor(attrIndex));
    }

    public StatisticsVisitor(Expression expression, StatisticsStrategy strategy)
            throws IllegalFilterException {
        this.expression = expression;
        this.strategy = strategy;
    }

    private void init(SimpleFeatureType schema, AttributeDescriptor attributeType) {
        expression = ff.property(attributeType.getLocalName());
        strategy = createStrategy(attributeType.getType().getBinding());
    }

    private static StatisticsStrategy createStrategy(Class<?> type) {
        if (type == Integer.class) {
            return new IntegerStrategy();
        } else if (type == Long.class) {
            return new LongStrategy();
        } else if (type == Float.class) {
            return new FloatStrategy();
        } else if (Number.class.isAssignableFrom(type)) {
            return new DoubleStrategy();
        } else if (String.class.isAssignableFrom(type)) {
            return new StringStrategy();
        }

        return null;
    }

    public void setNoData(Number noData) {
        if (strategy != null && noData != null)
            strategy.setNoData(noData);
    }

    public void reset() {
        if (strategy != null)
            strategy.reset();
    }

    public void visit(SimpleFeatureCollection features) {
        reset();

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                visit(featureIter.next());
            }
        } finally {
            featureIter.close();
        }
    }

    public void visit(SimpleFeature feature) {
        Object value = expression.evaluate(feature);
        if (strategy == null) {
            strategy = createStrategy(value.getClass());
        }
        strategy.add(value);
    }

    public StatisticsVisitorResult getResult() {
        return strategy == null ? new StatisticsVisitorResult() : strategy.getResult();
    }

    interface StatisticsStrategy {
        public void add(Object value);

        public StatisticsVisitorResult getResult();

        public void setNoData(Number noData);

        public void reset();
    }

    static class DoubleStrategy implements StatisticsStrategy {

        Double noData = null;

        int count = 0;

        int invalidCount = 0;

        Object firstValue = null;

        Object lastValue = null;

        double minVal = Double.MAX_VALUE;

        double maxVal = Double.MIN_VALUE;

        double sumOfVals = 0.0d;

        double sumOfSqrs = 0.0d;

        @Override
        public void add(Object value) {
            if (value == null) {
                invalidCount++;
                return;
            }

            // for BigDecimal, Double...
            double curVal = Double.valueOf(value.toString());
            if (Double.isNaN(curVal) || Double.isInfinite(curVal)) {
                invalidCount++;
                return;
            } else if (noData != null && SSUtils.compareDouble(curVal, noData)) {
                invalidCount++;
                return;
            }

            if (firstValue == null) {
                firstValue = value;
            }

            sumOfVals += curVal;
            sumOfSqrs += curVal * curVal;

            maxVal = Math.max(maxVal, curVal);
            minVal = Math.min(minVal, curVal);

            lastValue = value;

            count++;
        }

        @Override
        public StatisticsVisitorResult getResult() {
            StatisticsVisitorResult sr = new StatisticsVisitorResult();
            sr.setFirstValue(firstValue);
            sr.setLastValue(lastValue);
            sr.setCount(count);

            sr.setMinimum(minVal);
            sr.setMaximum(maxVal);
            sr.setSum(sumOfVals);
            sr.setNoData(noData);

            if (count > 0) {
                // Population Standard Deviation
                double variance = (sumOfSqrs - Math.pow(sumOfVals, 2.0) / count) / count;
                sr.setVariance(variance);
            }
            return sr;
        }

        @Override
        public void reset() {
            count = invalidCount = 0;
            firstValue = lastValue = null;
            minVal = Double.MAX_VALUE;
            maxVal = Double.MIN_VALUE;
            sumOfVals = sumOfSqrs = 0.0d;
        }

        @Override
        public void setNoData(Number noData) {
            this.noData = new Double(noData.doubleValue());
        }
    }

    static class FloatStrategy implements StatisticsStrategy {

        Float noData = null;

        int count = 0;

        int invalidCount = 0;

        Object firstValue = null;

        Object lastValue = null;

        float minVal = Float.MAX_VALUE;

        float maxVal = Float.MIN_VALUE;

        double sumOfVals = 0.0d;

        double sumOfSqrs = 0.0d;

        @Override
        public void add(Object value) {
            if (value == null) {
                invalidCount++;
                return;
            }

            float curVal = ((Float) value).floatValue();
            if (Float.isNaN(curVal) || Float.isInfinite(curVal)) {
                invalidCount++;
                return;
            } else if (noData != null && SSUtils.compareFloat(curVal, noData)) {
                invalidCount++;
                return;
            }

            if (firstValue == null) {
                firstValue = value;
            }

            sumOfVals += curVal;
            sumOfSqrs += curVal * curVal;

            maxVal = Math.max(maxVal, curVal);
            minVal = Math.min(minVal, curVal);

            lastValue = value;

            count++;
        }

        @Override
        public StatisticsVisitorResult getResult() {
            StatisticsVisitorResult sr = new StatisticsVisitorResult();
            sr.setFirstValue(firstValue);
            sr.setLastValue(lastValue);
            sr.setCount(count);

            sr.setMinimum(minVal);
            sr.setMaximum(maxVal);
            sr.setSum(sumOfVals);
            sr.setNoData(noData);

            if (count > 0) {
                // Population Standard Deviation
                double variance = (sumOfSqrs - Math.pow(sumOfVals, 2.0) / count) / count;
                sr.setVariance(variance);
            }
            return sr;
        }

        @Override
        public void reset() {
            count = invalidCount = 0;
            firstValue = lastValue = null;
            minVal = Float.MAX_VALUE;
            maxVal = Float.MIN_VALUE;
            sumOfVals = sumOfSqrs = 0.0d;
        }

        @Override
        public void setNoData(Number noData) {
            this.noData = new Float(noData.floatValue());
        }
    }

    static class LongStrategy implements StatisticsStrategy {

        Long noData = null;

        int count = 0;

        int invalidCount = 0;

        Object firstValue = null;

        Object lastValue = null;

        long minVal = Long.MAX_VALUE;

        long maxVal = Long.MIN_VALUE;

        double sumOfVals = 0.0d;

        double sumOfSqrs = 0.0d;

        @Override
        public void add(Object value) {
            if (value == null) {
                invalidCount++;
                return;
            }

            long curVal = ((Long) value).longValue();
            if (noData != null && curVal == noData) {
                invalidCount++;
                return;
            }

            if (firstValue == null) {
                firstValue = value;
            }

            sumOfVals += curVal;
            sumOfSqrs += curVal * curVal;

            maxVal = Math.max(maxVal, curVal);
            minVal = Math.min(minVal, curVal);

            lastValue = value;

            count++;
        }

        @Override
        public StatisticsVisitorResult getResult() {
            StatisticsVisitorResult sr = new StatisticsVisitorResult();
            sr.setFirstValue(firstValue);
            sr.setLastValue(lastValue);
            sr.setCount(count);

            sr.setMinimum(minVal);
            sr.setMaximum(maxVal);
            sr.setSum(sumOfVals);
            sr.setNoData(noData);

            if (count > 0) {
                // Population Standard Deviation
                double variance = (sumOfSqrs - Math.pow(sumOfVals, 2.0) / count) / count;
                sr.setVariance(variance);
            }
            return sr;
        }

        @Override
        public void reset() {
            count = invalidCount = 0;
            firstValue = lastValue = null;
            minVal = Long.MAX_VALUE;
            maxVal = Long.MIN_VALUE;
            sumOfVals = sumOfSqrs = 0.0d;
        }

        @Override
        public void setNoData(Number noData) {
            this.noData = new Long(noData.longValue());
        }
    }

    static class IntegerStrategy implements StatisticsStrategy {

        Integer noData = null;

        int count = 0;

        int invalidCount = 0;

        Object firstValue = null;

        Object lastValue = null;

        int minVal = Integer.MAX_VALUE;

        int maxVal = Integer.MIN_VALUE;

        double sumOfVals = 0.0d;

        double sumOfSqrs = 0.0d;

        @Override
        public void add(Object value) {
            if (value == null) {
                invalidCount++;
                return;
            }

            int curVal = ((Integer) value).intValue();
            if (noData != null && curVal == noData) {
                invalidCount++;
                return;
            }

            if (firstValue == null) {
                firstValue = value;
            }

            sumOfVals += curVal;
            sumOfSqrs += curVal * curVal;

            maxVal = Math.max(maxVal, curVal);
            minVal = Math.min(minVal, curVal);

            lastValue = value;

            count++;
        }

        @Override
        public StatisticsVisitorResult getResult() {
            StatisticsVisitorResult sr = new StatisticsVisitorResult();
            sr.setFirstValue(firstValue);
            sr.setLastValue(lastValue);
            sr.setCount(count);

            sr.setMinimum(minVal);
            sr.setMaximum(maxVal);
            sr.setSum(sumOfVals);
            sr.setNoData(noData);

            if (count > 0) {
                // Population Standard Deviation
                double variance = (sumOfSqrs - Math.pow(sumOfVals, 2.0) / count) / count;
                sr.setVariance(variance);
            }
            return sr;
        }

        @Override
        public void reset() {
            count = invalidCount = 0;
            firstValue = lastValue = null;
            minVal = Integer.MAX_VALUE;
            maxVal = Integer.MIN_VALUE;
            sumOfVals = sumOfSqrs = 0.0d;
        }

        @Override
        public void setNoData(Number noData) {
            this.noData = new Integer(noData.intValue());
        }
    }

    static class StringStrategy implements StatisticsStrategy {

        Object noData = null;

        int count = 0;

        int invalidCount = 0;

        Object firstValue = null;

        Object lastValue = null;

        @Override
        public void add(Object value) {
            if (value == null) {
                invalidCount++;
                return;
            }

            if (noData != null && noData.equals(value)) {
                invalidCount++;
                return;
            }

            if (firstValue == null) {
                firstValue = value;
            }

            lastValue = value;

            count++;
        }

        @Override
        public StatisticsVisitorResult getResult() {
            StatisticsVisitorResult sr = new StatisticsVisitorResult();
            sr.setFirstValue(firstValue);
            sr.setLastValue(lastValue);
            sr.setCount(count);
            sr.setNoData(noData);
            return sr;
        }

        @Override
        public void reset() {
            count = invalidCount = 0;
            firstValue = lastValue = null;
        }

        @Override
        public void setNoData(Number noData) {
            this.noData = noData;
        }
    }
}
