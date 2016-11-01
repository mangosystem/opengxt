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
 * RectangularBinningProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RectangularBinningProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RectangularBinningProcessFactory.class);

    private static final String PROCESS_NAME = "RectangularBinning";

    /*
     * RectangularBinning(SimpleFeatureCollection features, Expression weight, ReferencedEnvelope bbox, Integer columns, Integer rows, Boolean
     * validGrid): SimpleFeatureCollection
     */

    public RectangularBinningProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RectangularBinningProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RectangularBinning.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RectangularBinning.description");
    }

    /** features */
    public static final Parameter<SimpleFeatureCollection> features = new Parameter<SimpleFeatureCollection>(
            "features", SimpleFeatureCollection.class,
            getResource("RectangularBinning.features.title"),
            getResource("RectangularBinning.features.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "Point"));

    /** weight */
    public static final Parameter<Expression> weight = new Parameter<Expression>("weight",
            Expression.class, getResource("RectangularBinning.weight.title"),
            getResource("RectangularBinning.weight.description"), false, 0, 1, null, null);

    /** bbox */
    public static final Parameter<ReferencedEnvelope> bbox = new Parameter<ReferencedEnvelope>(
            "bbox", ReferencedEnvelope.class, getResource("RectangularBinning.bbox.title"),
            getResource("RectangularBinning.bbox.description"), false, 0, 1, null, null);

    /** width */
    public static final Parameter<Double> width = new Parameter<Double>("width", Double.class,
            getResource("RectangularBinning.width.title"),
            getResource("RectangularBinning.width.description"), true, 1, 1, null, null);

    /** height */
    public static final Parameter<Double> height = new Parameter<Double>("height", Double.class,
            getResource("RectangularBinning.height.title"),
            getResource("RectangularBinning.height.description"), true, 1, 1, null, null);

    /** validGrid */
    public static final Parameter<Boolean> validGrid = new Parameter<Boolean>("validGrid",
            Boolean.class, getResource("RectangularBinning.validGrid.title"),
            getResource("RectangularBinning.validGrid.description"), false, 0, 1, Boolean.TRUE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(features.key, features);
        parameterInfo.put(weight.key, weight);
        parameterInfo.put(bbox.key, bbox);
        parameterInfo.put(width.key, width);
        parameterInfo.put(height.key, height);
        parameterInfo.put(validGrid.key, validGrid);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("RectangularBinning.result.title"),
            getResource("RectangularBinning.result.description"));

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
