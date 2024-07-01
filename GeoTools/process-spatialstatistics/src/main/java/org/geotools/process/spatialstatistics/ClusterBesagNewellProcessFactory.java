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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation.FitnessFunctionType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * ClusterBesagNewellProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClusterBesagNewellProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(ClusterBesagNewellProcessFactory.class);

    private static final String PROCESS_NAME = "ClusterBesagNewell";

    /*
     * ClusterBesagNewell(SimpleFeatureCollection popFeatures, Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField, Integer
     * neighbours, FitnessFunctionType functionType, Double threshold): SimpleFeatureCollection, GridCoverage2D
     */

    public ClusterBesagNewellProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ClusterBesagNewellProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ClusterBesagNewell.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ClusterBesagNewell.description");
    }

    /** popFeatures */
    public static final Parameter<SimpleFeatureCollection> popFeatures = new Parameter<SimpleFeatureCollection>(
            "popFeatures", SimpleFeatureCollection.class,
            getResource("ClusterBesagNewell.popFeatures.title"),
            getResource("ClusterBesagNewell.popFeatures.description"), true, 1, 1, null, null);

    /** popField */
    public static final Parameter<Expression> popField = new Parameter<Expression>("popField",
            Expression.class, getResource("ClusterBesagNewell.popField.title"),
            getResource("ClusterBesagNewell.popField.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "popFeatures.Number"));

    /** caseFeatures */
    public static final Parameter<SimpleFeatureCollection> caseFeatures = new Parameter<SimpleFeatureCollection>(
            "caseFeatures", SimpleFeatureCollection.class,
            getResource("ClusterBesagNewell.caseFeatures.title"),
            getResource("ClusterBesagNewell.caseFeatures.description"), true, 1, 1, null, null);

    /** caseField */
    public static final Parameter<Expression> caseField = new Parameter<Expression>("caseField",
            Expression.class, getResource("ClusterBesagNewell.caseField.title"),
            getResource("ClusterBesagNewell.caseField.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "caseFeatures.Number"));

    /** neighbours */
    public static final Parameter<Integer> neighbours = new Parameter<Integer>("neighbours",
            Integer.class, getResource("ClusterBesagNewell.neighbours.title"),
            getResource("ClusterBesagNewell.neighbours.description"), false, 0, 1,
            Integer.valueOf(10), null);

    /** functionType */
    public static final Parameter<FitnessFunctionType> functionType = new Parameter<FitnessFunctionType>(
            "functionType", FitnessFunctionType.class,
            getResource("ClusterBesagNewell.functionType.title"),
            getResource("ClusterBesagNewell.functionType.description"), false, 0, 1,
            FitnessFunctionType.Poisson, null);

    /** threshold */
    public static final Parameter<Double> threshold = new Parameter<Double>("threshold",
            Double.class, getResource("ClusterBesagNewell.threshold.title"),
            getResource("ClusterBesagNewell.threshold.description"), false, 0, 1,
            Double.valueOf(0.01), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(popFeatures.key, popFeatures);
        parameterInfo.put(popField.key, popField);
        parameterInfo.put(caseFeatures.key, caseFeatures);
        parameterInfo.put(caseField.key, caseField);
        parameterInfo.put(neighbours.key, neighbours);
        parameterInfo.put(functionType.key, functionType);
        parameterInfo.put(threshold.key, threshold);
        return parameterInfo;
    }

    /** resultCircles */
    public static final Parameter<SimpleFeatureCollection> resultCircles = new Parameter<SimpleFeatureCollection>(
            "resultCircles", SimpleFeatureCollection.class,
            getResource("ClusterBesagNewell.resultCircles.title"),
            getResource("ClusterBesagNewell.resultCircles.description"), true, 1, 1, null, null);

    /** resultDensity */
    public static final Parameter<GridCoverage2D> resultDensity = new Parameter<GridCoverage2D>(
            "resultDensity", GridCoverage2D.class,
            getResource("ClusterBesagNewell.resultDensity.title"),
            getResource("ClusterBesagNewell.resultDensity.description"), true, 1, 1, null, null);

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(resultCircles.key, resultCircles);
        resultInfo.put(resultDensity.key, resultDensity);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
