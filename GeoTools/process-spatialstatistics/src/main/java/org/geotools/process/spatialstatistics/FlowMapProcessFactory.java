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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * FlowMapProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FlowMapProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FlowMapProcessFactory.class);

    private static final String PROCESS_NAME = "FlowMap";

    /*
     * FlowMap(SimpleFeatureCollection inputFeatures, Expression odValue, Expression doValue, Double maxSize): SimpleFeatureCollection
     */

    public FlowMapProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FlowMapProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FlowMap.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FlowMap.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("FlowMap.lineFeatures.title"),
            getResource("FlowMap.lineFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.LineString));

    /** odValue */
    public static final Parameter<Expression> odValue = new Parameter<Expression>("odValue",
            Expression.class, getResource("FlowMap.odValue.title"),
            getResource("FlowMap.odValue.description"), true, 1, 1, null, new KVP(Params.FIELD,
                    "lineFeatures.Number"));

    /** doValue */
    public static final Parameter<Expression> doValue = new Parameter<Expression>("doValue",
            Expression.class, getResource("FlowMap.doValue.title"),
            getResource("FlowMap.doValue.description"), false, 0, 1, null, new KVP(Params.FIELD,
                    "lineFeatures.Number"));

    /** maxSize */
    public static final Parameter<Double> maxSize = new Parameter<Double>("maxSize", Double.class,
            getResource("FlowMap.maxSize.title"), getResource("FlowMap.maxSize.description"),
            false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(odValue.key, odValue);
        parameterInfo.put(doValue.key, doValue);
        parameterInfo.put(maxSize.key, maxSize);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FlowMap.result.title"),
            getResource("FlowMap.result.description"), true, 1, 1, null, null);

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
