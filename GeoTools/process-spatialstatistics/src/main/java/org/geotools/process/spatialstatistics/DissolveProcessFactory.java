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
 * DissolveProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DissolveProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(DissolveProcessFactory.class);

    private static final String PROCESS_NAME = "Dissolve";

    /*
     * Dissolve(SimpleFeatureCollection inputFeatures, String dissolveField, String statisticsFields, Boolean useMultiPart) : SimpleFeatureCollection
     */

    public DissolveProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new DissolveProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Dissolve.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Dissolve.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("Dissolve.inputFeatures.title"),
            getResource("Dissolve.inputFeatures.description"), true, 1, 1, null, null);

    /** dissolveField */
    public static final Parameter<String> dissolveField = new Parameter<String>("dissolveField",
            String.class, getResource("Dissolve.dissolveField.title"),
            getResource("Dissolve.dissolveField.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** statisticsFields */
    public static final Parameter<String> statisticsFields = new Parameter<String>(
            "statisticsFields", String.class, getResource("Dissolve.statisticsFields.title"),
            getResource("Dissolve.statisticsFields.description"), false, 0, 1, null, null);

    /** useMultiPart */
    public static final Parameter<Boolean> useMultiPart = new Parameter<Boolean>("useMultiPart",
            Boolean.class, getResource("Dissolve.useMultiPart.title"),
            getResource("Dissolve.useMultiPart.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(dissolveField.key, dissolveField);
        parameterInfo.put(statisticsFields.key, statisticsFields);
        parameterInfo.put(useMultiPart.key, useMultiPart);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("Dissolve.result.title"),
            getResource("Dissolve.result.description"));

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
