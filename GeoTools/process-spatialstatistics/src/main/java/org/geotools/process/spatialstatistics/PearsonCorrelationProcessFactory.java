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
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * PearsonCorrelationProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PearsonCorrelationProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(PearsonCorrelationProcessFactory.class);

    private static final String PROCESS_NAME = "Pearson";

    /*
     * Pearson(SimpleFeatureCollection inputFeatures, String inputFields) : XML
     */

    public PearsonCorrelationProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new PearsonCorrelationProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Pearson.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Pearson.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("Pearson.inputFeatures.title"),
            getResource("Pearson.inputFeatures.description"), true, 1, 1, null, null);

    /** inputFields */
    public static final Parameter<String> inputFields = new Parameter<String>("inputFields",
            String.class, getResource("Pearson.inputFields.title"),
            getResource("Pearson.inputFields.description"), true, 1, 1, null, new KVP(
                    Params.FIELDS, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(inputFields.key, inputFields);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<PearsonResult> RESULT = new Parameter<PearsonResult>("result",
            PearsonResult.class, getResource("Pearson.result.title"),
            getResource("Pearson.result.description"));

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
