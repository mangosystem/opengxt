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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * RingMapProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RingMapProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RingMapProcessFactory.class);

    private static final String PROCESS_NAME = "RingMap";

    /*
     * RingMap(SimpleFeatureCollection inputFeatures, String fields, String targetField, Integer ringGap): SimpleFeatureCollection
     */

    public RingMapProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RingMapProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RingMap.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RingMap.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("RingMap.inputFeatures.title"),
            getResource("RingMap.inputFeatures.description"), true, 1, 1, null, null);

    /** fields */
    public static final Parameter<String> fields = new Parameter<String>("fields", String.class,
            getResource("RingMap.fields.title"), getResource("RingMap.fields.description"), true,
            1, 1, null, new KVP(Params.FIELDS, "inputFeatures.Number"));

    /** targetField */
    public static final Parameter<String> targetField = new Parameter<String>("targetField",
            String.class, getResource("RingMap.targetField.title"),
            getResource("RingMap.targetField.description"), false, 0, 1, "ring_val", new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** ringGap */
    public static final Parameter<Integer> ringGap = new Parameter<Integer>("ringGap",
            Integer.class, getResource("RingMap.ringGap.title"),
            getResource("RingMap.ringGap.description"), false, 0, 1, Integer.valueOf(1), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(fields.key, fields);
        parameterInfo.put(targetField.key, targetField);
        parameterInfo.put(ringGap.key, ringGap);
        return parameterInfo;
    }

    /** ringmap */
    public static final Parameter<SimpleFeatureCollection> ringmap = new Parameter<SimpleFeatureCollection>(
            "ringmap", SimpleFeatureCollection.class, getResource("RingMap.ringmap.title"),
            getResource("RingMap.ringmap.description"), true, 1, 1, null, new KVP(Params.STYLES,
                    "Quantile.targetField"));

    /** anchor */
    public static final Parameter<SimpleFeatureCollection> anchor = new Parameter<SimpleFeatureCollection>(
            "anchor", SimpleFeatureCollection.class, getResource("RingMap.anchor.title"),
            getResource("RingMap.anchor.description"), true, 1, 1, null, null);

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(anchor.key, anchor);
        resultInfo.put(ringmap.key, ringmap);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
