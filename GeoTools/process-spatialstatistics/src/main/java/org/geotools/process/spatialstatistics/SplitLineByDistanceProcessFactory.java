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
 * SplitLineByDistanceProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitLineByDistanceProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(SplitLineByDistanceProcessFactory.class);

    private static final String PROCESS_NAME = "SplitLineByDistance";

    // SplitLineByDistance(SimpleFeatureCollection lineFeatures, Expression distance): SimpleFeatureCollection

    public SplitLineByDistanceProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SplitLineByDistanceProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SplitLineByDistance.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SplitLineByDistance.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("SplitLineByDistance.lineFeatures.title"),
            getResource("SplitLineByDistance.lineFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "Polyline"));

    /** distance */
    public static final Parameter<Expression> distance = new Parameter<Expression>("distance",
            Expression.class, getResource("SplitLineByDistance.distance.title"),
            getResource("SplitLineByDistance.distance.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "lineFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(distance.key, distance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("SplitLineByDistance.result.title"),
            getResource("SplitLineByDistance.result.description"));

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
