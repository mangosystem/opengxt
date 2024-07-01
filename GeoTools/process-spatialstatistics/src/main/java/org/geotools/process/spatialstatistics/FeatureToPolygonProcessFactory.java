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
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * FeatureToPolygonProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureToPolygonProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FeatureToPolygonProcessFactory.class);

    private static final String PROCESS_NAME = "FeatureToPolygon";

    /*
     * FeatureToPolygon(SimpleFeatureCollection inputFeatures, Double tolerance, SimpleFeatureCollection labelFeatures): SimpleFeatureCollection
     */

    public FeatureToPolygonProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FeatureToPolygonProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FeatureToPolygon.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FeatureToPolygon.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("FeatureToPolygon.inputFeatures.title"),
            getResource("FeatureToPolygon.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polyline));

    /** tolerance */
    public static final Parameter<Double> tolerance = new Parameter<Double>("tolerance",
            Double.class, getResource("FeatureToPolygon.tolerance.title"),
            getResource("FeatureToPolygon.tolerance.description"), false, 0, 1,
            Double.valueOf(0.001d), null);

    /** labelFeatures */
    public static final Parameter<SimpleFeatureCollection> labelFeatures = new Parameter<SimpleFeatureCollection>(
            "labelFeatures", SimpleFeatureCollection.class,
            getResource("FeatureToPolygon.labelFeatures.title"),
            getResource("FeatureToPolygon.labelFeatures.description"), false, 0, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(tolerance.key, tolerance);
        parameterInfo.put(labelFeatures.key, labelFeatures);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FeatureToPolygon.result.title"),
            getResource("FeatureToPolygon.result.description"), true, 1, 1, null, null);

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
