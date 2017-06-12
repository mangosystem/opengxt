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
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName.PearsonItem;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.InvalidParameterValueException;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Calculates Pearson correlation coefficient
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PearsonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PearsonOperation.class);

    public PearsonOperation() {
    }

    public PearsonResult execute(SimpleFeatureCollection inputFeatures, String inputFields) {
        PearsonResult result = new PearsonResult();

        // inputFields = A, B, C, D, E
        List<String> fieldList = parseFields(inputFeatures.getSchema(), inputFields);
        if (fieldList.size() < 2) {
            throw new InvalidParameterValueException("Invalid parameters", "inputFields",
                    inputFields);
        }

        // calculate
        double[][] values = new double[fieldList.size()][fieldList.size()];
        for (int firstIndex = 0; firstIndex < fieldList.size(); firstIndex++) {
            String firstField = fieldList.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < fieldList.size(); secondIndex++) {
                String secondField = fieldList.get(secondIndex);
                double pearson = execute(inputFeatures, firstField, secondField);

                // firstIndex, secondIndex
                values[firstIndex][secondIndex] = pearson;
                values[secondIndex][firstIndex] = pearson;
            }
        }

        // result
        for (int firstIndex = 0; firstIndex < fieldList.size(); firstIndex++) {
            String firstField = fieldList.get(firstIndex);
            PropertyName property = new PropertyName(firstField);
            for (int secondIndex = 0; secondIndex < fieldList.size(); secondIndex++) {
                String secondField = fieldList.get(secondIndex);

                PearsonItem item = new PearsonItem(secondField);
                if (firstIndex != secondIndex) {
                    double value = values[firstIndex][secondIndex];
                    item.setValue(new Double(value));
                }
                property.getItems().add(item);
            }
            result.getProeprtyNames().add(property);
        }

        return result;
    }

    private double execute(SimpleFeatureCollection inputFeatures, String fieldName1,
            String fieldName2) {
        double sumX = 0.0;
        double sumY = 0.0;

        double sumXSq = 0.0;
        double sumYSq = 0.0;
        double pSum = 0.0;
        int rowCount = 0;

        Expression obsExpression = ff.property(fieldName1);
        Expression popExpression = ff.property(fieldName2);

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // evaluate and validate
                Double dxVal = obsExpression.evaluate(feature, Double.class);
                Double dyVal = popExpression.evaluate(feature, Double.class);
                if (dxVal == null || dyVal == null) {
                    continue;
                }

                double dx = dxVal.doubleValue();
                double dy = dyVal.doubleValue();
                if (Double.isNaN(dx) || Double.isInfinite(dx) || Double.isNaN(dy)
                        || Double.isInfinite(dy)) {
                    continue;
                }

                sumX += dx;
                sumY += dy;

                sumXSq += Math.pow(dx, 2.0);
                sumYSq += Math.pow(dy, 2.0);

                pSum += dx * dy;

                rowCount++;
            }
        } finally {
            featureIter.close();
        }

        if (rowCount == 0) {
            return 0;
        }

        double x = (sumXSq - Math.pow(sumX, 2) / rowCount);
        double y = (sumYSq - Math.pow(sumY, 2) / rowCount);
        double den = Math.pow(x * y, 0.5);
        if (den == 0) {
            return 0;
        }

        double num = pSum - ((sumX * sumY) / rowCount);
        return num / den;
    }

    private List<String> parseFields(SimpleFeatureType schema, String inputFields) {
        List<String> fieldList = new ArrayList<String>();

        // comma separated fields
        String[] fields = inputFields.split(",");
        for (int k = 0; k < fields.length; k++) {
            String fieldName = FeatureTypes.validateProperty(schema, fields[k].trim());
            if (schema.indexOf(fieldName) != -1 && !fieldList.contains(fieldName)) {
                fieldList.add(fieldName);
            }
        }

        return fieldList;
    }

    public static final class PearsonResult {

        @XStreamImplicit
        List<PropertyName> proeprtyNames = new ArrayList<PropertyName>();

        public List<PropertyName> getProeprtyNames() {
            return proeprtyNames;
        }

        public void setProeprtyNames(List<PropertyName> proeprtyNames) {
            this.proeprtyNames = proeprtyNames;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            for (PropertyName proeprtyName : proeprtyNames) {
                sb.append(proeprtyName.toString()).append(separator);
            }

            return sb.toString();
        }

        public static class PropertyName {
            @XStreamAsAttribute
            String name;

            @XStreamImplicit
            List<PearsonItem> items = new ArrayList<PearsonItem>();

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public List<PearsonItem> getItems() {
                return items;
            }

            public void setItems(List<PearsonItem> items) {
                this.items = items;
            }

            public PropertyName(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                final String separator = System.getProperty("line.separator");

                StringBuffer sb = new StringBuffer();
                sb.append("Name: ").append(name).append(separator);
                for (PearsonItem pearsonItem : items) {
                    sb.append("  ").append(pearsonItem.toString()).append(separator);
                }

                return sb.toString();
            }

            // version 1.4.2 @XStreamConverter(value=ToAttributedValueConverter.class,
            // strings={"value"})
            public static class PearsonItem {
                @XStreamAsAttribute
                String name;

                Double value = Double.valueOf(1d);

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public Double getValue() {
                    return value;
                }

                public void setValue(Double value) {
                    this.value = value;
                }

                public PearsonItem(String name) {
                    this.name = name;
                }

                @Override
                public String toString() {
                    final String separator = System.getProperty("line.separator");

                    StringBuffer sb = new StringBuffer();
                    sb.append("    Name: ").append(name).append(separator);
                    sb.append("      Value: ").append(value).append(separator);

                    return sb.toString();
                }
            }
        }
    }

}
