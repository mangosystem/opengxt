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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * CollectEventsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CollectEventsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CountFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "CollectEvents";

    // CollectEvents (SimpleFeatureCollection inputFeatures, String countField)

    public CollectEventsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CollectEventsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CollectEvents.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CollectEvents.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CollectEvents.inputFeatures.title"),
            getResource("CollectEvents.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "Point"));

    /** countField */
    public static final Parameter<String> countField = new Parameter<String>("countField",
            String.class, getResource("CollectEvents.countField.title"),
            getResource("CollectEvents.countField.description"), false, 0, 1, "icount", new KVP(
                    Parameter.OPTIONS, "inputFeatures.Number"));

    /** tolerance */
    public static final Parameter<Double> tolerance = new Parameter<Double>("tolerance",
            Double.class, getResource("CollectEvents.tolerance.title"),
            getResource("CollectEvents.tolerance.description"), false, 0, 1, Double.valueOf(0.1d),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(countField.key, countField);
        parameterInfo.put(tolerance.key, tolerance);
        return parameterInfo;
    }

    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CollectEvents.result.title"),
            getResource("CollectEvents.result.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "EqualInterval.countField"));

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
