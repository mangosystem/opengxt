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
 * RandomPointsPerFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RandomPointsPerFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RandomPointsPerFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "RandomPointsPerFeatures";

    // RandomPointsPerFeatures(SimpleFeatureCollection polygonFeatures, Expression expression, Integer pointCount): SimpleFeatureCollection

    public RandomPointsPerFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RandomPointsPerFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RandomPointsPerFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RandomPointsPerFeatures.description");
    }

    /** polygonFeatures */
    public static final Parameter<SimpleFeatureCollection> polygonFeatures = new Parameter<SimpleFeatureCollection>(
            "polygonFeatures", SimpleFeatureCollection.class,
            getResource("RandomPointsPerFeatures.polygonFeatures.title"),
            getResource("RandomPointsPerFeatures.polygonFeatures.description"), true, 1, 1, null,
            new KVP(Parameter.FEATURE_TYPE, "Polygon"));

    /** expression */
    public static final Parameter<Expression> expression = new Parameter<Expression>("expression",
            Expression.class, getResource("RandomPointsPerFeatures.expression.title"),
            getResource("RandomPointsPerFeatures.expression.description"), false, 0, 1, null, null);

    /** pointCount */
    public static final Parameter<Integer> pointCount = new Parameter<Integer>("pointCount",
            Integer.class, getResource("RandomPointsPerFeatures.pointCount.title"),
            getResource("RandomPointsPerFeatures.pointCount.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(polygonFeatures.key, polygonFeatures);
        parameterInfo.put(expression.key, expression);
        parameterInfo.put(pointCount.key, pointCount);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("RandomPointsPerFeatures.result.title"),
            getResource("RandomPointsPerFeatures.result.description"));

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
