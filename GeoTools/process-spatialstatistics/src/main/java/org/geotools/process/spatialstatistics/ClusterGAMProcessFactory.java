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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation.FitnessFunctionType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * ClusterGAMProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClusterGAMProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ClusterGAMProcessFactory.class);

    private static final String PROCESS_NAME = "ClusterGAM";

    /*
     * ClusterGAM(SimpleFeatureCollection popFeatures, Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField, Double
     * minRadius, Double maxRadius, Double radiusIncrement, Double overlapRatio, FitnessFunctionType functionType, Double threshold):
     * SimpleFeatureCollection, GridCoverage2D
     */

    public ClusterGAMProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ClusterGAMProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ClusterGAM.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ClusterGAM.description");
    }

    /** popFeatures */
    public static final Parameter<SimpleFeatureCollection> popFeatures = new Parameter<SimpleFeatureCollection>(
            "popFeatures", SimpleFeatureCollection.class,
            getResource("ClusterGAM.popFeatures.title"),
            getResource("ClusterGAM.popFeatures.description"), true, 1, 1, null, null);

    /** popField */
    public static final Parameter<Expression> popField = new Parameter<Expression>("popField",
            Expression.class, getResource("ClusterGAM.popField.title"),
            getResource("ClusterGAM.popField.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "popFeatures.Number"));

    /** caseFeatures */
    public static final Parameter<SimpleFeatureCollection> caseFeatures = new Parameter<SimpleFeatureCollection>(
            "caseFeatures", SimpleFeatureCollection.class,
            getResource("ClusterGAM.caseFeatures.title"),
            getResource("ClusterGAM.caseFeatures.description"), true, 1, 1, null, null);

    /** caseField */
    public static final Parameter<Expression> caseField = new Parameter<Expression>("caseField",
            Expression.class, getResource("ClusterGAM.caseField.title"),
            getResource("ClusterGAM.caseField.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "caseFeatures.Number"));

    /** minRadius */
    public static final Parameter<Double> minRadius = new Parameter<Double>("minRadius",
            Double.class, getResource("ClusterGAM.minRadius.title"),
            getResource("ClusterGAM.minRadius.description"), false, 0, 1, Double.valueOf(0d), null);

    /** maxRadius */
    public static final Parameter<Double> maxRadius = new Parameter<Double>("maxRadius",
            Double.class, getResource("ClusterGAM.maxRadius.title"),
            getResource("ClusterGAM.maxRadius.description"), false, 0, 1, Double.valueOf(0d), null);

    /** radiusIncrement */
    public static final Parameter<Double> radiusIncrement = new Parameter<Double>("radiusIncrement",
            Double.class, getResource("ClusterGAM.radiusIncrement.title"),
            getResource("ClusterGAM.radiusIncrement.description"), false, 0, 1, Double.valueOf(0d),
            null);

    /** overlapRatio */
    public static final Parameter<Double> overlapRatio = new Parameter<Double>("overlapRatio",
            Double.class, getResource("ClusterGAM.overlapRatio.title"),
            getResource("ClusterGAM.overlapRatio.description"), false, 0, 1, Double.valueOf(0.5d),
            null);

    /** functionType */
    public static final Parameter<FitnessFunctionType> functionType = new Parameter<FitnessFunctionType>(
            "functionType", FitnessFunctionType.class, getResource("ClusterGAM.functionType.title"),
            getResource("ClusterGAM.functionType.description"), false, 0, 1,
            FitnessFunctionType.Poisson, null);

    /** threshold */
    public static final Parameter<Double> threshold = new Parameter<Double>("threshold",
            Double.class, getResource("ClusterGAM.threshold.title"),
            getResource("ClusterGAM.threshold.description"), false, 0, 1, Double.valueOf(0.01),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(popFeatures.key, popFeatures);
        parameterInfo.put(popField.key, popField);
        parameterInfo.put(caseFeatures.key, caseFeatures);
        parameterInfo.put(caseField.key, caseField);
        parameterInfo.put(minRadius.key, minRadius);
        parameterInfo.put(maxRadius.key, maxRadius);
        parameterInfo.put(radiusIncrement.key, radiusIncrement);
        parameterInfo.put(overlapRatio.key, overlapRatio);
        parameterInfo.put(functionType.key, functionType);
        parameterInfo.put(threshold.key, threshold);
        return parameterInfo;
    }

    /** resultCircles */
    public static final Parameter<SimpleFeatureCollection> resultCircles = new Parameter<SimpleFeatureCollection>(
            "resultCircles", SimpleFeatureCollection.class,
            getResource("ClusterGAM.resultCircles.title"),
            getResource("ClusterGAM.resultCircles.description"), true, 1, 1, null, null);

    /** resultDensity */
    public static final Parameter<GridCoverage2D> resultDensity = new Parameter<GridCoverage2D>(
            "resultDensity", GridCoverage2D.class, getResource("ClusterGAM.resultDensity.title"),
            getResource("ClusterGAM.resultDensity.description"), true, 1, 1, null, null);

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
