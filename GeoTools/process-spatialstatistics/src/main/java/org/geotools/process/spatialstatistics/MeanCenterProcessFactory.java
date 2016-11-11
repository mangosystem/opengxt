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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * MeanCenterProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MeanCenterProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(MeanCenterProcessFactory.class);

    private static final String PROCESS_NAME = "MeanCenter";

    /*
     * MeanCenter(SimpleFeatureCollection inputFeatures, String weightField, String caseField, String dimensionField): SimpleFeatureCollection
     */

    public MeanCenterProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new MeanCenterProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("MeanCenter.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("MeanCenter.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("MeanCenter.inputFeatures.title"),
            getResource("MeanCenter.inputFeatures.description"), true, 1, 1, null, null);

    /** weightField */
    public static final Parameter<String> weightField = new Parameter<String>("weightField",
            String.class, getResource("MeanCenter.weightField.title"),
            getResource("MeanCenter.weightField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** caseField */
    public static final Parameter<String> caseField = new Parameter<String>("caseField",
            String.class, getResource("MeanCenter.caseField.title"),
            getResource("MeanCenter.caseField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** dimensionField */
    public static final Parameter<String> dimensionField = new Parameter<String>("dimensionField",
            String.class, getResource("MeanCenter.dimensionField.title"),
            getResource("MeanCenter.dimensionField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(weightField.key, weightField);
        parameterInfo.put(caseField.key, caseField);
        parameterInfo.put(dimensionField.key, dimensionField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("MeanCenter.result.title"),
            getResource("MeanCenter.result.description"));

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
