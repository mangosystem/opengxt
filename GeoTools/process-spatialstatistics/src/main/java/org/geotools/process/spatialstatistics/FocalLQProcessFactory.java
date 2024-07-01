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

import org.geotools.api.data.Parameter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * FocalLQProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FocalLQProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FocalLQProcessFactory.class);

    private static final String PROCESS_NAME = "FocalLQ";

    /*
     * FocalLQ(SimpleFeatureCollection GML, Expression fieldName1, Expression fieldName2, SpatialConcept spatialConcept, DistanceMethod
     * distanceMethod, searchDistance Double): SimpleFeatureCollection
     */

    public FocalLQProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FocalLQProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FocalLQ.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FocalLQ.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("FocalLQ.inputFeatures.title"),
            getResource("FocalLQ.inputFeatures.description"), true, 1, 1, null, null);

    /** xField */
    public static final Parameter<Expression> xField = new Parameter<Expression>("xField",
            Expression.class, getResource("FocalLQ.xField.title"),
            getResource("FocalLQ.xField.description"), true, 1, 1, null, new KVP(Params.FIELD,
                    "inputFeatures.Number"));

    /** yField */
    public static final Parameter<Expression> yField = new Parameter<Expression>("yField",
            Expression.class, getResource("FocalLQ.yField.title"),
            getResource("FocalLQ.yField.description"), true, 1, 1, null, new KVP(Params.FIELD,
                    "inputFeatures.Number"));

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class, getResource("FocalLQ.spatialConcept.title"),
            getResource("FocalLQ.spatialConcept.description"), false, 0, 1,
            SpatialConcept.FixedDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class, getResource("FocalLQ.distanceMethod.title"),
            getResource("FocalLQ.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("FocalLQ.searchDistance.title"),
            getResource("FocalLQ.searchDistance.description"), false, 0, 1, Double.valueOf(0.0),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(xField.key, xField);
        parameterInfo.put(yField.key, yField);
        parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(searchDistance.key, searchDistance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FocalLQ.result.title"),
            getResource("FocalLQ.result.description"));

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
