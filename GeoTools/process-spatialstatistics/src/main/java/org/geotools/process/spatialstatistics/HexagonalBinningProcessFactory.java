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
 * HexagonalBinningProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HexagonalBinningProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(HexagonalBinningProcessFactory.class);

    private static final String PROCESS_NAME = "HexagonalBinning";

    /*
     * HexagonalBinning(SimpleFeatureCollection features, Expression weight, ReferencedEnvelope bbox, Double size, Boolean validGrid):
     * SimpleFeatureCollection
     */

    public HexagonalBinningProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new HexagonalBinningProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("HexagonalBinning.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("HexagonalBinning.description");
    }

    /** features */
    public static final Parameter<SimpleFeatureCollection> features = new Parameter<SimpleFeatureCollection>(
            "features", SimpleFeatureCollection.class,
            getResource("HexagonalBinning.features.title"),
            getResource("HexagonalBinning.features.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "Point"));

    /** weight */
    public static final Parameter<Expression> weight = new Parameter<Expression>("weight",
            Expression.class, getResource("HexagonalBinning.weight.title"),
            getResource("HexagonalBinning.weight.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "features.Number"));

    /** bbox */
    public static final Parameter<ReferencedEnvelope> bbox = new Parameter<ReferencedEnvelope>(
            "bbox", ReferencedEnvelope.class, getResource("HexagonalBinning.bbox.title"),
            getResource("HexagonalBinning.bbox.description"), false, 0, 1, null, null);

    /** size */
    public static final Parameter<Double> size = new Parameter<Double>("size", Double.class,
            getResource("HexagonalBinning.size.title"),
            getResource("HexagonalBinning.size.description"), true, 1, 1, null, null);

    /** validGrid */
    public static final Parameter<Boolean> validGrid = new Parameter<Boolean>("validGrid",
            Boolean.class, getResource("HexagonalBinning.validGrid.title"),
            getResource("HexagonalBinning.validGrid.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(features.key, features);
        parameterInfo.put(weight.key, weight);
        parameterInfo.put(bbox.key, bbox);
        parameterInfo.put(size.key, size);
        parameterInfo.put(validGrid.key, validGrid);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("HexagonalBinning.result.title"),
            getResource("HexagonalBinning.result.description"));

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
