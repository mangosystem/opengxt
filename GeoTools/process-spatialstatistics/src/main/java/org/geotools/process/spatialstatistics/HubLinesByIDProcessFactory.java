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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * HubLinesByIDProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HubLinesByIDProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByIDProcessFactory.class);

    private static final String PROCESS_NAME = "HubLinesByID";

    /*
     * HubLinesByID(SimpleFeatureCollection hubFeatures, String hubIdField, SimpleFeatureCollection spokeFeatures, String spokeIdField, boolean
     * preserveAttributes, boolean useCentroid, double maximumDistance): SimpleFeatureCollection
     */

    public HubLinesByIDProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new HubLinesByIDProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("HubLinesByID.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("HubLinesByID.description");
    }

    /** hubFeatures */
    public static final Parameter<SimpleFeatureCollection> hubFeatures = new Parameter<SimpleFeatureCollection>(
            "hubFeatures", SimpleFeatureCollection.class,
            getResource("HubLinesByID.hubFeatures.title"),
            getResource("HubLinesByID.hubFeatures.description"), true, 1, 1, null, null);

    /** hubIdField */
    public static final Parameter<String> hubIdField = new Parameter<String>("hubIdField",
            String.class, getResource("HubLinesByID.hubIdField.title"),
            getResource("HubLinesByID.hubIdField.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "hubFeatures.All"));

    /** spokeFeatures */
    public static final Parameter<SimpleFeatureCollection> spokeFeatures = new Parameter<SimpleFeatureCollection>(
            "spokeFeatures", SimpleFeatureCollection.class,
            getResource("HubLinesByID.spokeFeatures.title"),
            getResource("HubLinesByID.spokeFeatures.description"), true, 1, 1, null, null);

    /** spokeIdField */
    public static final Parameter<String> spokeIdField = new Parameter<String>("spokeIdField",
            String.class, getResource("HubLinesByID.spokeIdField.title"),
            getResource("HubLinesByID.spokeIdField.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "spokeFeatures.All"));

    /** preserveAttributes */
    public static final Parameter<Boolean> preserveAttributes = new Parameter<Boolean>(
            "preserveAttributes", Boolean.class,
            getResource("HubLinesByID.preserveAttributes.title"),
            getResource("HubLinesByID.preserveAttributes.description"), false, 0, 1, Boolean.TRUE,
            null);

    /** useCentroid */
    public static final Parameter<Boolean> useCentroid = new Parameter<Boolean>("useCentroid",
            Boolean.class, getResource("HubLinesByID.useCentroid.title"),
            getResource("HubLinesByID.useCentroid.description"), false, 0, 1, Boolean.TRUE, null);

    /** maximumDistance */
    public static final Parameter<Double> maximumDistance = new Parameter<Double>(
            "maximumDistance", Double.class, getResource("HubLinesByID.maximumDistance.title"),
            getResource("HubLinesByID.maximumDistance.description"), false, 0, 1,
            Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(hubFeatures.key, hubFeatures);
        parameterInfo.put(hubIdField.key, hubIdField);
        parameterInfo.put(spokeFeatures.key, spokeFeatures);
        parameterInfo.put(spokeIdField.key, spokeIdField);
        parameterInfo.put(preserveAttributes.key, preserveAttributes);
        parameterInfo.put(useCentroid.key, useCentroid);
        parameterInfo.put(maximumDistance.key, maximumDistance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("HubLinesByID.result.title"),
            getResource("HubLinesByID.result.description"), true, 1, 1, null, null);

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
