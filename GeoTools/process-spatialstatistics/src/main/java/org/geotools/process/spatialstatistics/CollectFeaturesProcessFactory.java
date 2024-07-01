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
 * CollectFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CollectFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CollectFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "CollectFeatures";

    // CollectFeatures(SimpleFeatureCollection inputFeatures, String countField, Double tolerance): SimpleFeatureCollection

    public CollectFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CollectFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CollectFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CollectFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CollectFeatures.inputFeatures.title"),
            getResource("CollectFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** countField */
    public static final Parameter<String> countField = new Parameter<String>("countField",
            String.class, getResource("CollectFeatures.countField.title"),
            getResource("CollectFeatures.countField.description"), false, 0, 1, "icount", new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** tolerance */
    public static final Parameter<Double> tolerance = new Parameter<Double>("tolerance",
            Double.class, getResource("CollectFeatures.tolerance.title"),
            getResource("CollectFeatures.tolerance.description"), false, 0, 1,
            Double.valueOf(0.1d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(countField.key, countField);
        parameterInfo.put(tolerance.key, tolerance);
        return parameterInfo;
    }

    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CollectFeatures.result.title"),
            getResource("CollectFeatures.result.description"), true, 1, 1, null, new KVP(
                    Params.STYLES, "EqualInterval.countField"));

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
