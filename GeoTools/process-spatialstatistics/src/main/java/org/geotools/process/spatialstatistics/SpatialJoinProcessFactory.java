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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.enumeration.SpatialJoinType;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * SpatialJoinProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialJoinProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SpatialJoinProcessFactory.class);

    private static final String PROCESS_NAME = "SpatialJoin";

    /*
     * SpatialJoin(SimpleFeatureCollection inputFeatures, SimpleFeatureCollection joinFeatures, SpatialJoinType joinType, Double searchRadius, DistanceUnit radiusUnit) :
     * SimpleFeatureCollection
     */

    public SpatialJoinProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SpatialJoinProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SpatialJoin.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SpatialJoin.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("SpatialJoin.inputFeatures.title"),
            getResource("SpatialJoin.inputFeatures.description"), true, 1, 1, null, null);

    /** joinFeatures */
    public static final Parameter<SimpleFeatureCollection> joinFeatures = new Parameter<SimpleFeatureCollection>(
            "joinFeatures", SimpleFeatureCollection.class,
            getResource("SpatialJoin.joinFeatures.title"),
            getResource("SpatialJoin.joinFeatures.description"), true, 1, 1, null, null);

    /** joinType */
    public static final Parameter<SpatialJoinType> joinType = new Parameter<SpatialJoinType>(
            "joinType", SpatialJoinType.class, getResource("SpatialJoin.joinType.title"),
            getResource("SpatialJoin.joinType.description"), false, 0, 1,
            SpatialJoinType.KeepAllRecord, null);

    /** searchRadius */
    public static final Parameter<Double> searchRadius = new Parameter<Double>("searchRadius",
            Double.class, getResource("SpatialJoin.searchRadius.title"),
            getResource("SpatialJoin.searchRadius.description"), false, 0, 1, Double.valueOf(0.0d),
            null);

    /** radiusUnit */
    public static final Parameter<DistanceUnit> radiusUnit = new Parameter<DistanceUnit>(
            "radiusUnit", DistanceUnit.class, getResource("SpatialJoin.radiusUnit.title"),
            getResource("SpatialJoin.radiusUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(joinFeatures.key, joinFeatures);
        parameterInfo.put(joinType.key, joinType);
        parameterInfo.put(searchRadius.key, searchRadius);
        parameterInfo.put(radiusUnit.key, radiusUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("SpatialJoin.result.title"),
            getResource("SpatialJoin.result.description"));

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
