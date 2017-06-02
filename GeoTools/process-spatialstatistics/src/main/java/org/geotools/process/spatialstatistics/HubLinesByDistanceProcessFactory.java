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
 * HubLinesByDistanceProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HubLinesByDistanceProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(HubLinesByDistanceProcessFactory.class);

    private static final String PROCESS_NAME = "HubLinesByDistance";

    /*
     * HubLinesByDistance(SimpleFeatureCollection hubFeatures, String hubIdField, SimpleFeatureCollection spokeFeatures, Boolean preserveAttributes,
     * Boolean useCentroid, Boolean useBezierCurve, Double maximumDistance): SimpleFeatureCollection
     */

    public HubLinesByDistanceProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new HubLinesByDistanceProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("HubLinesByDistance.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("HubLinesByDistance.description");
    }

    /** hubFeatures */
    public static final Parameter<SimpleFeatureCollection> hubFeatures = new Parameter<SimpleFeatureCollection>(
            "hubFeatures", SimpleFeatureCollection.class,
            getResource("HubLinesByDistance.hubFeatures.title"),
            getResource("HubLinesByDistance.hubFeatures.description"), true, 1, 1, null, null);

    /** hubIdField */
    public static final Parameter<String> hubIdField = new Parameter<String>("hubIdField",
            String.class, getResource("HubLinesByDistance.hubIdField.title"),
            getResource("HubLinesByDistance.hubIdField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "hubFeatures.All"));

    /** spokeFeatures */
    public static final Parameter<SimpleFeatureCollection> spokeFeatures = new Parameter<SimpleFeatureCollection>(
            "spokeFeatures", SimpleFeatureCollection.class,
            getResource("HubLinesByDistance.spokeFeatures.title"),
            getResource("HubLinesByDistance.spokeFeatures.description"), true, 1, 1, null, null);

    /** preserveAttributes */
    public static final Parameter<Boolean> preserveAttributes = new Parameter<Boolean>(
            "preserveAttributes", Boolean.class,
            getResource("HubLinesByDistance.preserveAttributes.title"),
            getResource("HubLinesByDistance.preserveAttributes.description"), false, 0, 1,
            Boolean.TRUE, null);

    /** useCentroid */
    public static final Parameter<Boolean> useCentroid = new Parameter<Boolean>("useCentroid",
            Boolean.class, getResource("HubLinesByDistance.useCentroid.title"),
            getResource("HubLinesByDistance.useCentroid.description"), false, 0, 1, Boolean.TRUE,
            null);

    /** useBezierCurve */
    public static final Parameter<Boolean> useBezierCurve = new Parameter<Boolean>(
            "useBezierCurve", Boolean.class,
            getResource("HubLinesByDistance.useBezierCurve.title"),
            getResource("HubLinesByDistance.useBezierCurve.description"), false, 0, 1,
            Boolean.FALSE, null);

    /** maximumDistance */
    public static final Parameter<Double> maximumDistance = new Parameter<Double>(
            "maximumDistance", Double.class,
            getResource("HubLinesByDistance.maximumDistance.title"),
            getResource("HubLinesByDistance.maximumDistance.description"), false, 0, 1,
            Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(hubFeatures.key, hubFeatures);
        parameterInfo.put(hubIdField.key, hubIdField);
        parameterInfo.put(spokeFeatures.key, spokeFeatures);
        parameterInfo.put(preserveAttributes.key, preserveAttributes);
        parameterInfo.put(useCentroid.key, useCentroid);
        parameterInfo.put(useBezierCurve.key, useBezierCurve);
        parameterInfo.put(maximumDistance.key, maximumDistance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("HubLinesByDistance.result.title"),
            getResource("HubLinesByDistance.result.description"), true, 1, 1, null, null);

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
