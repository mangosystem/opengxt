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
import org.geotools.process.spatialstatistics.GlobalLeesLProcess.LeesLProcessResult;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * GlobalLeesLProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalLeesLProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(GlobalLeesLProcessFactory.class);

    private static final String PROCESS_NAME = "GlobalLeesL";

    /*
     * GlobalLeesL(SimpleFeatureCollection inputFeatures, String xField, String yField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
     * StandardizationMethod standardization, Double searchDistance, Boolean selfNeighbors) : LeesL
     */

    public GlobalLeesLProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new GlobalLeesLProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("GlobalLeesL.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("GlobalLeesL.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("GlobalLeesL.inputFeatures.title"),
            getResource("GlobalLeesL.inputFeatures.description"), true, 1, 1, null, null);

    /** xField */
    public static final Parameter<String> xField = new Parameter<String>("xField", String.class,
            getResource("GlobalLeesL.xField.title"), getResource("GlobalLeesL.xField.description"),
            true, 1, 1, null, new KVP(Params.FIELD, "inputFeatures.Number"));

    /** yField */
    public static final Parameter<String> yField = new Parameter<String>("yField", String.class,
            getResource("GlobalLeesL.yField.title"), getResource("GlobalLeesL.yField.description"),
            true, 1, 1, null, new KVP(Params.FIELD, "inputFeatures.Number"));

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class,
            getResource("GlobalLeesL.spatialConcept.title"),
            getResource("GlobalLeesL.spatialConcept.description"), false, 0, 1,
            SpatialConcept.InverseDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("GlobalLeesL.distanceMethod.title"),
            getResource("GlobalLeesL.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** standardization */
    public static final Parameter<StandardizationMethod> standardization = new Parameter<StandardizationMethod>(
            "standardization", StandardizationMethod.class,
            getResource("GlobalLeesL.standardization.title"),
            getResource("GlobalLeesL.standardization.description"), false, 0, 1,
            StandardizationMethod.None, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("GlobalLeesL.searchDistance.title"),
            getResource("GlobalLeesL.searchDistance.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    /** selfNeighbors */
    public static final Parameter<Boolean> selfNeighbors = new Parameter<Boolean>("selfNeighbors",
            Boolean.class, getResource("GlobalLeesL.selfNeighbors.title"),
            getResource("GlobalLeesL.selfNeighbors.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(xField.key, xField);
        parameterInfo.put(yField.key, yField);
        parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(standardization.key, standardization);
        parameterInfo.put(searchDistance.key, searchDistance);
        parameterInfo.put(selfNeighbors.key, selfNeighbors);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<LeesLProcessResult> RESULT = new Parameter<LeesLProcessResult>(
            "result", LeesLProcessResult.class, getResource("GlobalLeesL.result.title"),
            getResource("GlobalLeesL.result.description"));

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
