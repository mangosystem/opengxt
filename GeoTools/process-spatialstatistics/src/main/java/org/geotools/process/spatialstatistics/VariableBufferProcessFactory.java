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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * VariableBufferProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class VariableBufferProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(VariableBufferProcessFactory.class);

    private static final String PROCESS_NAME = "VariableBuffer";

    /*
     * VariableBuffer(SimpleFeatureCollection lineFeatures, Expression startDistance, Expression endDistance, DistanceUnit distanceUnit):
     * SimpleFeatureCollection
     */

    public VariableBufferProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new VariableBufferProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("VariableBuffer.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("VariableBuffer.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("VariableBuffer.lineFeatures.title"),
            getResource("VariableBuffer.lineFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.LineString));

    /** startDistance */
    public static final Parameter<Expression> startDistance = new Parameter<Expression>(
            "startDistance", Expression.class, getResource("VariableBuffer.startDistance.title"),
            getResource("VariableBuffer.startDistance.description"), false, 0, 1, ff.literal(0d),
            null);

    /** endDistance */
    public static final Parameter<Expression> endDistance = new Parameter<Expression>("endDistance",
            Expression.class, getResource("VariableBuffer.endDistance.title"),
            getResource("VariableBuffer.endDistance.description"), true, 1, 1, null, null);

    /** distanceUnit */
    public static final Parameter<DistanceUnit> distanceUnit = new Parameter<DistanceUnit>(
            "distanceUnit", DistanceUnit.class, getResource("VariableBuffer.distanceUnit.title"),
            getResource("VariableBuffer.distanceUnit.description"), false, 0, 1, DistanceUnit.Default,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(startDistance.key, startDistance);
        parameterInfo.put(endDistance.key, endDistance);
        parameterInfo.put(distanceUnit.key, distanceUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("VariableBuffer.result.title"),
            getResource("VariableBuffer.result.description"), true, 1, 1, null, null);

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
