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
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * LocalRogersonRProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalRogersonRProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(LocalRogersonRProcessFactory.class);

    private static final String PROCESS_NAME = "LocalRogersonR";

    /*
     * LocalRogersonR(SimpleFeatureCollection inputFeatures, String xField, String yField, SpatialConcept spatialConcept, DistanceMethod
     * distanceMethod, StandardizationMethod standardization, Double searchDistance, Double kappa) : SimpleFeatureCollection
     */

    public LocalRogersonRProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new LocalRogersonRProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("LocalRogersonR.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("LocalRogersonR.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("LocalRogersonR.inputFeatures.title"),
            getResource("LocalRogersonR.inputFeatures.description"), true, 1, 1, null, null);

    /** xField */
    public static final Parameter<String> xField = new Parameter<String>("xField", String.class,
            getResource("LocalRogersonR.xField.title"),
            getResource("LocalRogersonR.xField.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** yField */
    public static final Parameter<String> yField = new Parameter<String>("yField", String.class,
            getResource("LocalRogersonR.yField.title"),
            getResource("LocalRogersonR.yField.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class,
            getResource("LocalRogersonR.spatialConcept.title"),
            getResource("LocalRogersonR.spatialConcept.description"), false, 0, 1,
            SpatialConcept.InverseDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("LocalRogersonR.distanceMethod.title"),
            getResource("LocalRogersonR.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** standardization */
    public static final Parameter<StandardizationMethod> standardization = new Parameter<StandardizationMethod>(
            "standardization", StandardizationMethod.class,
            getResource("LocalRogersonR.standardization.title"),
            getResource("LocalRogersonR.standardization.description"), false, 0, 1,
            StandardizationMethod.None, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("LocalRogersonR.searchDistance.title"),
            getResource("LocalRogersonR.searchDistance.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    /** kappa */
    public static final Parameter<Double> kappa = new Parameter<Double>("kappa", Double.class,
            getResource("LocalRogersonR.kappa.title"),
            getResource("LocalRogersonR.kappa.description"), false, 0, 1, Double.valueOf(1.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(xField.key, xField);
        parameterInfo.put(yField.key, yField);
        // parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        // parameterInfo.put(standardization.key, standardization);
        // parameterInfo.put(searchDistance.key, searchDistance);
        parameterInfo.put(kappa.key, kappa);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("LocalRogersonR.result.title"),
            getResource("LocalRogersonR.result.description"));

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
