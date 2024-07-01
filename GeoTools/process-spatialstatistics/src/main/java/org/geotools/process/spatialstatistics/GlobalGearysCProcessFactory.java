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
import org.geotools.process.spatialstatistics.GlobalGearysCProcess.GearysCProcessResult;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * GlobalGearysCProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalGearysCProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGearysCProcessFactory.class);

    private static final String PROCESS_NAME = "GlobalGearysC";

    /*
     * GlobalGearysC(SimpleFeatureCollection inputFeatures, String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
     * StandardizationMethod standardization, Double searchDistance, Boolean selfNeighbors) : GearysC
     */

    public GlobalGearysCProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new GlobalGearysCProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("GlobalGearysC.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("GlobalGearysC.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("GlobalGearysC.inputFeatures.title"),
            getResource("GlobalGearysC.inputFeatures.description"), true, 1, 1, null, null);

    /** inputField */
    public static final Parameter<String> inputField = new Parameter<String>("inputField",
            String.class, getResource("GlobalGearysC.inputField.title"),
            getResource("GlobalGearysC.inputField.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class,
            getResource("GlobalGearysC.spatialConcept.title"),
            getResource("GlobalGearysC.spatialConcept.description"), false, 0, 1,
            SpatialConcept.InverseDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("GlobalGearysC.distanceMethod.title"),
            getResource("GlobalGearysC.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** standardization */
    public static final Parameter<StandardizationMethod> standardization = new Parameter<StandardizationMethod>(
            "standardization", StandardizationMethod.class,
            getResource("GlobalGearysC.standardization.title"),
            getResource("GlobalGearysC.standardization.description"), false, 0, 1,
            StandardizationMethod.None, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("GlobalGearysC.searchDistance.title"),
            getResource("GlobalGearysC.searchDistance.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    /** selfNeighbors */
    public static final Parameter<Boolean> selfNeighbors = new Parameter<Boolean>("selfNeighbors",
            Boolean.class, getResource("GlobalGearysC.selfNeighbors.title"),
            getResource("GlobalGearysC.selfNeighbors.description"), false, 0, 1, Boolean.FALSE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(inputField.key, inputField);
        parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(standardization.key, standardization);
        parameterInfo.put(searchDistance.key, searchDistance);
        // parameterInfo.put(selfNeighbors.key, selfNeighbors);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GearysCProcessResult> RESULT = new Parameter<GearysCProcessResult>(
            "result", GearysCProcessResult.class, getResource("GlobalGearysC.result.title"),
            getResource("GlobalGearysC.result.description"));

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
