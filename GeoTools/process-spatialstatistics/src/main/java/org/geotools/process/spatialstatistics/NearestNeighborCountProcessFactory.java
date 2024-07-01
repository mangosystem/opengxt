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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * NearestNeighborCountProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NearestNeighborCountProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(NearestNeighborCountProcessFactory.class);

    private static final String PROCESS_NAME = "NearestNeighborCount";

    /*
     * NearestNeighborCount(SimpleFeatureCollection inputFeatures, String countField, SimpleFeatureCollection nearFeatures, Double searchRadius,
     * DistanceUnit radiusUnit): SimpleFeatureCollection
     */

    public NearestNeighborCountProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new NearestNeighborCountProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("NearestNeighborCount.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("NearestNeighborCount.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("NearestNeighborCount.inputFeatures.title"),
            getResource("NearestNeighborCount.inputFeatures.description"), true, 1, 1, null, null);

    /** countField */
    public static final Parameter<String> countField = new Parameter<String>("countField",
            String.class, getResource("NearestNeighborCount.countField.title"),
            getResource("NearestNeighborCount.countField.description"), false, 0, 1,
            String.valueOf("count"), new KVP(Params.FIELD, "inputFeatures.All"));

    /** nearFeatures */
    public static final Parameter<SimpleFeatureCollection> nearFeatures = new Parameter<SimpleFeatureCollection>(
            "nearFeatures", SimpleFeatureCollection.class,
            getResource("NearestNeighborCount.nearFeatures.title"),
            getResource("NearestNeighborCount.nearFeatures.description"), true, 1, 1, null, null);

    /** searchRadius */
    public static final Parameter<Double> searchRadius = new Parameter<Double>("searchRadius",
            Double.class, getResource("NearestNeighborCount.searchRadius.title"),
            getResource("NearestNeighborCount.searchRadius.description"), true, 1, 1,
            Double.valueOf(0d), null);

    /** radiusUnit */
    public static final Parameter<DistanceUnit> radiusUnit = new Parameter<DistanceUnit>(
            "radiusUnit", DistanceUnit.class, getResource("NearestNeighborCount.radiusUnit.title"),
            getResource("NearestNeighborCount.radiusUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(countField.key, countField);
        parameterInfo.put(nearFeatures.key, nearFeatures);
        parameterInfo.put(searchRadius.key, searchRadius);
        parameterInfo.put(radiusUnit.key, radiusUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("NearestNeighborCount.result.title"),
            getResource("NearestNeighborCount.result.description"), true, 1, 1, null, null);

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
