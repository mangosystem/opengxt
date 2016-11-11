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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * CircularBinningProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircularBinningProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CircularBinningProcessFactory.class);

    private static final String PROCESS_NAME = "CircularBinning";

    /*
     * CircularBinning(SimpleFeatureCollection features, Expression weight, ReferencedEnvelope bbox, Double radius, Boolean validGrid):
     * SimpleFeatureCollection
     */

    public CircularBinningProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CircularBinningProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CircularBinning.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CircularBinning.description");
    }

    /** features */
    public static final Parameter<SimpleFeatureCollection> features = new Parameter<SimpleFeatureCollection>(
            "features", SimpleFeatureCollection.class,
            getResource("CircularBinning.features.title"),
            getResource("CircularBinning.features.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "Point"));

    /** weight */
    public static final Parameter<Expression> weight = new Parameter<Expression>("weight",
            Expression.class, getResource("CircularBinning.weight.title"),
            getResource("CircularBinning.weight.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "features.Number"));

    /** bbox */
    public static final Parameter<ReferencedEnvelope> bbox = new Parameter<ReferencedEnvelope>(
            "bbox", ReferencedEnvelope.class, getResource("CircularBinning.bbox.title"),
            getResource("CircularBinning.bbox.description"), false, 0, 1, null, null);

    /** radius */
    public static final Parameter<Double> radius = new Parameter<Double>("radius", Double.class,
            getResource("CircularBinning.radius.title"),
            getResource("CircularBinning.radius.description"), true, 1, 1, null, null);

    /** validGrid */
    public static final Parameter<Boolean> validGrid = new Parameter<Boolean>("validGrid",
            Boolean.class, getResource("CircularBinning.validGrid.title"),
            getResource("CircularBinning.validGrid.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(features.key, features);
        parameterInfo.put(weight.key, weight);
        parameterInfo.put(bbox.key, bbox);
        parameterInfo.put(radius.key, radius);
        parameterInfo.put(validGrid.key, validGrid);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CircularBinning.result.title"),
            getResource("CircularBinning.result.description"), true, 1, 1, null, new KVP(
                    Params.STYLES, "EqualInterval.val"));

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
