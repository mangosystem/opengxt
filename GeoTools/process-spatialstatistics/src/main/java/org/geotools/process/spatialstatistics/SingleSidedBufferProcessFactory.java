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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * SingleSidedBufferProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SingleSidedBufferProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SingleSidedBufferProcessFactory.class);

    private static final String PROCESS_NAME = "SingleSidedBuffer";

    /*
     * SingleSidedBuffer(SimpleFeatureCollection inputFeatures, Expression distance, DistanceUnit distanceUnit, Integer quadrantSegments):
     * SimpleFeatureCollection
     */

    public SingleSidedBufferProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SingleSidedBufferProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SingleSidedBuffer.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SingleSidedBuffer.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("SingleSidedBuffer.inputFeatures.title"),
            getResource("SingleSidedBuffer.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.LineString));

    /** distance */
    public static final Parameter<Expression> distance = new Parameter<Expression>("distance",
            Expression.class, getResource("SingleSidedBuffer.distance.title"),
            getResource("SingleSidedBuffer.distance.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** distanceUnit */
    public static final Parameter<DistanceUnit> distanceUnit = new Parameter<DistanceUnit>(
            "distanceUnit", DistanceUnit.class,
            getResource("SingleSidedBuffer.distanceUnit.title"),
            getResource("SingleSidedBuffer.distanceUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    /** quadrantSegments */
    public static final Parameter<Integer> quadrantSegments = new Parameter<Integer>(
            "quadrantSegments", Integer.class,
            getResource("SingleSidedBuffer.quadrantSegments.title"),
            getResource("SingleSidedBuffer.quadrantSegments.description"), false, 0, 1,
            Integer.valueOf(8), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distance.key, distance);
        parameterInfo.put(distanceUnit.key, distanceUnit);
        parameterInfo.put(quadrantSegments.key, quadrantSegments);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("SingleSidedBuffer.result.title"),
            getResource("SingleSidedBuffer.result.description"), true, 1, 1, null, null);

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
