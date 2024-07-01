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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * OffsetFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OffsetFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(OffsetFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "OffsetFeatures";

    /*
     * OffsetFeatures(SimpleFeatureCollection inputFeatures, Expression offsetX, Expression offsetY): SimpleFeatureCollection
     */

    public OffsetFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new OffsetFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("OffsetFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("OffsetFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("OffsetFeatures.inputFeatures.title"),
            getResource("OffsetFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** offsetX */
    public static final Parameter<Expression> offsetX = new Parameter<Expression>("offsetX",
            Expression.class, getResource("OffsetFeatures.offsetX.title"),
            getResource("OffsetFeatures.offsetX.description"), true, 0, 1, ff.literal(0d), new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** offsetY */
    public static final Parameter<Expression> offsetY = new Parameter<Expression>("offsetY",
            Expression.class, getResource("OffsetFeatures.offsetY.title"),
            getResource("OffsetFeatures.offsetY.description"), true, 0, 1, ff.literal(0d), new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(offsetX.key, offsetX);
        parameterInfo.put(offsetY.key, offsetY);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("OffsetFeatures.result.title"),
            getResource("OffsetFeatures.result.description"));

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
