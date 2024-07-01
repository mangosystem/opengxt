/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.util.logging.Logging;

/**
 * ConcaveHullProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ConcaveHullProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ConcaveHullProcessFactory.class);

    private static final String PROCESS_NAME = "ConcaveHull";

    /*
     * ConcaveHull(SimpleFeatureCollection features, Expression group, Double alpha, Boolean removeHoles, Boolean splitMultipart):
     * SimpleFeatureCollection
     */

    public ConcaveHullProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ConcaveHullProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ConcaveHull.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ConcaveHull.description");
    }

    /** features */
    public static final Parameter<SimpleFeatureCollection> features = new Parameter<SimpleFeatureCollection>(
            "features", SimpleFeatureCollection.class, getResource("ConcaveHull.features.title"),
            getResource("ConcaveHull.features.description"), true, 1, 1, null, null);

    /** group */
    public static final Parameter<Expression> group = new Parameter<Expression>("group",
            Expression.class, getResource("ConcaveHull.group.title"),
            getResource("ConcaveHull.group.description"), false, 0, 1, null, null);

    /** alpha */
    public static final Parameter<Double> alpha = new Parameter<Double>("alpha", Double.class,
            getResource("ConcaveHull.alpha.title"), getResource("ConcaveHull.alpha.description"),
            false, 0, 1, Double.valueOf(0.3), null);

    /** removeHoles */
    public static final Parameter<Boolean> removeHoles = new Parameter<Boolean>("removeHoles",
            Boolean.class, getResource("ConcaveHull.removeHoles.title"),
            getResource("ConcaveHull.removeHoles.description"), false, 0, 1, Boolean.FALSE, null);

    /** splitMultipart */
    public static final Parameter<Boolean> splitMultipart = new Parameter<Boolean>("splitMultipart",
            Boolean.class, getResource("ConcaveHull.splitMultipart.title"),
            getResource("ConcaveHull.splitMultipart.description"), false, 0, 1, Boolean.FALSE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(features.key, features);
        parameterInfo.put(group.key, group);
        parameterInfo.put(alpha.key, alpha);
        parameterInfo.put(removeHoles.key, removeHoles);
        parameterInfo.put(splitMultipart.key, splitMultipart);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ConcaveHull.result.title"),
            getResource("ConcaveHull.result.description"), true, 1, 1, null, null);

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
