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
import org.geotools.process.spatialstatistics.enumeration.ThiessenAttributeMode;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * ThiessenPolygonProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThiessenPolygonProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ThiessenPolygonProcessFactory.class);

    private static final String PROCESS_NAME = "ThiessenPolygon";

    /*
     * ThiessenPolygon(SimpleFeatureCollection inputFeatures, ThiessenAttributeMode attributes, Geometry clipArea) : SimpleFeatureCollection
     * attributes: ONLY_FID(Default), ALL
     */

    public ThiessenPolygonProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ThiessenPolygonProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Thiessen.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Thiessen.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("Thiessen.inputFeatures.title"),
            getResource("Thiessen.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** attributes */
    public static final Parameter<ThiessenAttributeMode> attributes = new Parameter<ThiessenAttributeMode>(
            "attributes", ThiessenAttributeMode.class, getResource("Thiessen.attributes.title"),
            getResource("Thiessen.attributes.description"), false, 0, 1,
            ThiessenAttributeMode.OnlyFID, null);

    /** clipArea */
    public static final Parameter<Geometry> clipArea = new Parameter<Geometry>("clipArea",
            Geometry.class, getResource("Thiessen.clipArea.title"),
            getResource("Thiessen.clipArea.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(attributes.key, attributes);
        parameterInfo.put(clipArea.key, clipArea);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("Thiessen.result.title"),
            getResource("Thiessen.result.description"));

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
