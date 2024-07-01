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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * WedgeBufferProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WedgeBufferProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(WedgeBufferProcessFactory.class);

    private static final String PROCESS_NAME = "WedgeBuffer";

    /*
     * WedgeBuffer(SimpleFeatureCollection pointFeatures, Expression azimuth, Expression wedgeAngle, Expression innerRadius, Expression outerRadius,
     * DistanceUnit radiusUnit): SimpleFeatureCollection
     */

    public WedgeBufferProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new WedgeBufferProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("WedgeBuffer.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("WedgeBuffer.description");
    }

    /** pointFeatures */
    public static final Parameter<SimpleFeatureCollection> pointFeatures = new Parameter<SimpleFeatureCollection>(
            "pointFeatures", SimpleFeatureCollection.class,
            getResource("WedgeBuffer.pointFeatures.title"),
            getResource("WedgeBuffer.pointFeatures.description"), true, 1, 1, null, null);

    /** azimuth */
    public static final Parameter<Expression> azimuth = new Parameter<Expression>("azimuth",
            Expression.class, getResource("WedgeBuffer.azimuth.title"),
            getResource("WedgeBuffer.azimuth.description"), true, 1, 1, null, new KVP(Params.FIELD,
                    "pointFeatures.Number"));

    /** wedgeAngle */
    public static final Parameter<Expression> wedgeAngle = new Parameter<Expression>("wedgeAngle",
            Expression.class, getResource("WedgeBuffer.wedgeAngle.title"),
            getResource("WedgeBuffer.wedgeAngle.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "pointFeatures.Number"));

    /** innerRadius */
    public static final Parameter<Expression> innerRadius = new Parameter<Expression>(
            "innerRadius", Expression.class, getResource("WedgeBuffer.innerRadius.title"),
            getResource("WedgeBuffer.innerRadius.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "pointFeatures.Number"));

    /** outerRadius */
    public static final Parameter<Expression> outerRadius = new Parameter<Expression>(
            "outerRadius", Expression.class, getResource("WedgeBuffer.outerRadius.title"),
            getResource("WedgeBuffer.outerRadius.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "pointFeatures.Number"));

    /** radiusUnit */
    public static final Parameter<DistanceUnit> radiusUnit = new Parameter<DistanceUnit>(
            "radiusUnit", DistanceUnit.class, getResource("WedgeBuffer.radiusUnit.title"),
            getResource("WedgeBuffer.radiusUnit.description"), false, 0, 1, DistanceUnit.Default,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(pointFeatures.key, pointFeatures);
        parameterInfo.put(azimuth.key, azimuth);
        parameterInfo.put(wedgeAngle.key, wedgeAngle);
        parameterInfo.put(innerRadius.key, innerRadius);
        parameterInfo.put(outerRadius.key, outerRadius);
        parameterInfo.put(radiusUnit.key, radiusUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("WedgeBuffer.result.title"),
            getResource("WedgeBuffer.result.description"), true, 1, 1, null, null);

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
