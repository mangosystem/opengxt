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
 * BufferExpressionProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BufferExpressionProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(BufferExpressionProcessFactory.class);

    private static final String PROCESS_NAME = "BufferFeatures";

    /*
     * BufferFeatures(SimpleFeatureCollection inputFeatures, Expression distance, int quadrantSegments): SimpleFeatureCollection
     */

    public BufferExpressionProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new BufferExpressionProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("BufferFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("BufferFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("BufferFeatures.inputFeatures.title"),
            getResource("BufferFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** distance */
    public static final Parameter<Expression> distance = new Parameter<Expression>("distance",
            Expression.class, getResource("BufferFeatures.distance.title"),
            getResource("BufferFeatures.distance.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** quadrantSegments */
    public static final Parameter<Integer> quadrantSegments = new Parameter<Integer>(
            "quadrantSegments", Integer.class,
            getResource("BufferFeatures.quadrantSegments.title"),
            getResource("BufferFeatures.quadrantSegments.description"), false, 0, 1,
            Integer.valueOf(8), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distance.key, distance);
        parameterInfo.put(quadrantSegments.key, quadrantSegments);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("BufferFeatures.result.title"),
            getResource("BufferFeatures.result.description"), true, 1, 1, null, null);

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
