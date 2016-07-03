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
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * CentralFeatureFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CentralFeatureProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CentralFeatureProcessFactory.class);

    private static final String PROCESS_NAME = "CentralFeature";

    /*
     * CentralFeature(SimpleFeatureCollection inputFeatures, DistanceMethod distanceMethod, String weightField, String selfPotentialWeightField,
     * String caseField): SimpleFeatureCollection
     */

    public CentralFeatureProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CentralFeatureProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CentralFeature.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CentralFeature.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CentralFeature.inputFeatures.title"),
            getResource("CentralFeature.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "All"));

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("CentralFeature.distanceMethod.title"),
            getResource("CentralFeature.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** weightField */
    public static final Parameter<String> weightField = new Parameter<String>("weightField",
            String.class, getResource("CentralFeature.weightField.title"),
            getResource("CentralFeature.weightField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** selfPotentialWeightField */
    public static final Parameter<String> selfPotentialWeightField = new Parameter<String>(
            "selfPotentialWeightField", String.class,
            getResource("CentralFeature.selfPotentialWeightField.title"),
            getResource("CentralFeature.selfPotentialWeightField.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** caseField */
    public static final Parameter<String> caseField = new Parameter<String>("caseField",
            String.class, getResource("CentralFeature.caseField.title"),
            getResource("CentralFeature.caseField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(weightField.key, weightField);
        parameterInfo.put(selfPotentialWeightField.key, selfPotentialWeightField);
        parameterInfo.put(caseField.key, caseField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CentralFeature.result.title"),
            getResource("CentralFeature.result.description"));

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
