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
 * SimplifyProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SimplifyProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SimplifyProcessFactory.class);

    private static final String PROCESS_NAME = "Simplify";

    /*
     * Simplify(SimpleFeatureCollection inputFeatures, Expression tolerance, Boolean preserveTopology): SimpleFeatureCollection
     */

    public SimplifyProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SimplifyProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Simplify.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Simplify.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("Simplify.inputFeatures.title"),
            getResource("Simplify.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polyline));

    /** tolerance */
    public static final Parameter<Expression> tolerance = new Parameter<Expression>("tolerance",
            Expression.class, getResource("Simplify.tolerance.title"),
            getResource("Simplify.tolerance.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** preserveTopology */
    public static final Parameter<Boolean> preserveTopology = new Parameter<Boolean>(
            "preserveTopology", Boolean.class, getResource("Simplify.preserveTopology.title"),
            getResource("Simplify.preserveTopology.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(tolerance.key, tolerance);
        parameterInfo.put(preserveTopology.key, preserveTopology);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("Simplify.result.title"),
            getResource("Simplify.result.description"));

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
