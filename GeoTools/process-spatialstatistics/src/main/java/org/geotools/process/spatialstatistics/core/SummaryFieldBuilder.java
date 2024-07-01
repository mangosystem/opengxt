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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.process.spatialstatistics.enumeration.StaticsType;
import org.geotools.util.logging.Logging;

/**
 * Summary Field Builder
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class SummaryFieldBuilder {
    protected static final Logger LOGGER = Logging.getLogger(SummaryFieldBuilder.class);

    static final SummaryFieldBuilder INSTANCE = new SummaryFieldBuilder();

    public static SummaryFieldBuilder getInstance() {
        return INSTANCE;
    }

    // FIRST, LAST, SUM, MEAN, MIN, MAX, RANGE, STD, VAR, COUNT
    // FIRST(Fst), LAST(Lst), SUM, MEAN(Avg, Average), MIN(Minimum),
    // MAX(Maximum), RANGE, STD(StandardDeviation), VAR(Variance), COUNT(Cnt)

    public List<StatisticsField> buildFields(SimpleFeatureType srcType, String summaryFields,
            String targetFields) {
        List<StatisticsField> statList = new ArrayList<StatisticsField>();

        if (summaryFields == null || summaryFields.length() == 0) {
            return statList;
        }

        String[] arrSFields = summaryFields.split(",");
        String[] arrTFields = targetFields.split(",");
        if (arrSFields.length != arrTFields.length) {
            LOGGER.log(Level.FINE, "summaryFields != targetFields");
            return statList;
        }

        int length = Math.min(arrSFields.length, arrTFields.length);
        for (int k = 0; k < length; k++) {
            String curStat = arrSFields[k].trim();
            if (!curStat.contains(".")) {
                LOGGER.log(Level.FINE, curStat + " field does not contain [.] character!");
                return null;
            }

            String[] arrFields = curStat.split("\\.");
            String statType = arrFields[0].trim().toUpperCase();
            String sourceField = FeatureTypes.validateProperty(srcType, arrFields[1].trim());
            String targetField = arrTFields[k].trim();

            AttributeDescriptor attDesc = srcType.getDescriptor(sourceField);
            int fieldLength = FeatureTypes.getAttributeLength(attDesc);

            Class<?> fieldType = Double.class;
            StatisticsField curItem = new StatisticsField();
            StaticsType staticsType = StaticsType.Count;

            if (statType.contains("FIRST") || statType.contains("FST")) {
                fieldType = attDesc.getType().getBinding();
                staticsType = StaticsType.First;
            } else if (statType.contains("LAST") || statType.contains("LST")) {
                fieldType = attDesc.getType().getBinding();
                staticsType = StaticsType.Last;
            } else if (statType.contains("SUM")) {
                staticsType = StaticsType.Sum;
            } else if (statType.contains("MEAN") || statType.contains("AVE")
                    || statType.contains("AVG")) {
                staticsType = StaticsType.Mean;
            } else if (statType.contains("MIN")) {
                staticsType = StaticsType.Minimum;
            } else if (statType.contains("MAX")) {
                staticsType = StaticsType.Maximum;
            } else if (statType.contains("RANGE") || statType.contains("RNG")) {
                staticsType = StaticsType.Range;
            } else if (statType.contains("STD") || statType.contains("ST")) {
                staticsType = StaticsType.StandardDeviation;
            } else if (statType.contains("VAR")) {
                staticsType = StaticsType.Variance;
            } else if (statType.contains("COUNT") || statType.contains("CNT")) {
                fieldType = Integer.class;
                staticsType = StaticsType.Count;
            } else {
                LOGGER.log(Level.WARNING, curStat + "'s " + statType + " type not supported!");
                return null;
            }

            curItem.setSrcField(sourceField);
            curItem.setTargetField(targetField);
            curItem.setStatType(staticsType);
            curItem.fieldType = fieldType;

            curItem.setFieldLength(fieldLength);

            statList.add(curItem);
        }

        return statList;
    }

    public List<StatisticsField> buildFields(SimpleFeatureType srcType, String summaryFields) {
        List<StatisticsField> statList = new ArrayList<StatisticsField>();
        if (summaryFields == null || summaryFields.length() == 0) {
            return statList;
        }

        StringBuffer targetFields = new StringBuffer();
        String[] arrSFields = summaryFields.split(",");
        for (int k = 0; k < arrSFields.length; k++) {
            String[] arrFields = arrSFields[k].trim().split("\\.");
            String statType = arrFields[0].trim().toUpperCase();
            String sourceField = arrFields[1].trim();

            String targetField = String.format("%s_%s", statType, sourceField);
            if (statType.contains("FIRST") || statType.contains("FST")) {
                targetField = String.format("fst_%s", sourceField);
            } else if (statType.contains("LAST") || statType.contains("LST")) {
                targetField = String.format("lST_%s", sourceField);
            } else if (statType.contains("SUM")) {
                targetField = String.format("sum_%s", sourceField);
            } else if (statType.contains("MEAN") || statType.contains("AVE")
                    || statType.contains("AVG")) {
                targetField = String.format("avg_%s", sourceField);
            } else if (statType.contains("MIN")) {
                targetField = String.format("min_%s", sourceField);
            } else if (statType.contains("MAX")) {
                targetField = String.format("max_%s", sourceField);
            } else if (statType.contains("RANGE") || statType.contains("RNG")) {
                targetField = String.format("rng_%s", sourceField);
            } else if (statType.contains("STD") || statType.contains("ST")) {
                targetField = String.format("std_%s", sourceField);
            } else if (statType.contains("VAR")) {
                targetField = String.format("var_%s", sourceField);
            } else if (statType.contains("COUNT") || statType.contains("CNT")) {
                targetField = String.format("cnt_%s", sourceField);
            }

            if (targetFields.length() == 0) {
                targetFields.append(targetField);
            } else {
                targetFields.append(",");
                targetFields.append(targetField);
            }
        }

        return buildFields(srcType, summaryFields, targetFields.toString());
    }

}
