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
package org.geotools.process.spatialstatistics;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * StandardizedScoresProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardizedScoresProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(StandardizedScoresProcessFactory.class);

    private static final String PROCESS_NAME = "StandardizedScores";

    /*
     * StandardizedScores(SimpleFeatureCollection inputFeatures, Expression xField, Expression yField, String targetField): SimpleFeatureCollection
     */

    public StandardizedScoresProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new StandardizedScoresProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("StandardizedScores.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("StandardizedScores.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("StandardizedScores.inputFeatures.title"),
            getResource("StandardizedScores.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "All"));

    /** xField */
    public static final Parameter<Expression> xField = new Parameter<Expression>("xField",
            Expression.class, getResource("StandardizedScores.xField.title"),
            getResource("StandardizedScores.xField.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "inputFeatures.Number"));

    /** yField */
    public static final Parameter<Expression> yField = new Parameter<Expression>("yField",
            Expression.class, getResource("StandardizedScores.yField.title"),
            getResource("StandardizedScores.yField.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "inputFeatures.Number"));

    /** targetField */
    public static final Parameter<String> targetField = new Parameter<String>("targetField",
            String.class, getResource("StandardizedScores.targetField.title"),
            getResource("StandardizedScores.targetField.description"), false, 0, 1, "std_scr",
            new KVP(Parameter.OPTIONS, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(xField.key, xField);
        parameterInfo.put(yField.key, yField);
        parameterInfo.put(targetField.key, targetField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("StandardizedScores.result.title"),
            getResource("StandardizedScores.result.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "Quantile.targetField"));

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(RESULT.key, RESULT);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
