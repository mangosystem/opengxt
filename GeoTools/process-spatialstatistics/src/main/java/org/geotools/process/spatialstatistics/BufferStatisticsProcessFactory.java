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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * BufferStatisticsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BufferStatisticsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(BufferStatisticsProcessFactory.class);

    private static final String PROCESS_NAME = "BufferPointStatistics";

    /*
     * BufferPointStatistics(SimpleFeatureCollection inputFeatures, Double distance, DistanceUnit distanceUnit, SimpleFeatureCollection pointFeatures,
     * String countField, String statisticsFields): SimpleFeatureCollection
     */

    public BufferStatisticsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new BufferStatisticsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("BufferPointStatistics.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("BufferPointStatistics.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("BufferPointStatistics.inputFeatures.title"),
            getResource("BufferPointStatistics.inputFeatures.description"), true, 1, 1, null, null);

    /** distance */
    public static final Parameter<Double> distance = new Parameter<Double>("distance", Double.class,
            getResource("BufferPointStatistics.distance.title"),
            getResource("BufferPointStatistics.distance.description"), true, 1, 1,
            Double.valueOf(0.0d), null);

    /** distanceUnit */
    public static final Parameter<DistanceUnit> distanceUnit = new Parameter<DistanceUnit>(
            "distanceUnit", DistanceUnit.class,
            getResource("BufferPointStatistics.distanceUnit.title"),
            getResource("BufferPointStatistics.distanceUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    /** pointFeatures */
    public static final Parameter<SimpleFeatureCollection> pointFeatures = new Parameter<SimpleFeatureCollection>(
            "pointFeatures", SimpleFeatureCollection.class,
            getResource("BufferPointStatistics.pointFeatures.title"),
            getResource("BufferPointStatistics.pointFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Point));

    /** countField */
    public static final Parameter<String> countField = new Parameter<String>("countField",
            String.class, getResource("BufferPointStatistics.countField.title"),
            getResource("BufferPointStatistics.countField.description"), false, 0, 1, "count",
            null);

    /** statisticsFields */
    public static final Parameter<String> statisticsFields = new Parameter<String>(
            "statisticsFields", String.class,
            getResource("BufferPointStatistics.statisticsFields.title"),
            getResource("BufferPointStatistics.statisticsFields.description"), false, 0, 1, null,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distance.key, distance);
        parameterInfo.put(distanceUnit.key, distanceUnit);
        parameterInfo.put(pointFeatures.key, pointFeatures);
        parameterInfo.put(countField.key, countField);
        parameterInfo.put(statisticsFields.key, statisticsFields);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("BufferPointStatistics.result.title"),
            getResource("BufferPointStatistics.result.description"), true, 1, 1, null,
            new KVP(Params.STYLES, "EqualInterval.countField"));

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
