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
 * ClipWithFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClipWithFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ClipWithFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "ClipWithFeatures";

    /*
     * ClipWithFeatures(SimpleFeatureCollection inputFeatures, SimpleFeatureCollection clipFeatures): SimpleFeatureCollection
     */

    public ClipWithFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ClipWithFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ClipWithFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ClipWithFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("ClipWithFeatures.inputFeatures.title"),
            getResource("ClipWithFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** clipFeatures */
    public static final Parameter<SimpleFeatureCollection> clipFeatures = new Parameter<SimpleFeatureCollection>(
            "clipFeatures", SimpleFeatureCollection.class,
            getResource("ClipWithFeatures.clipFeatures.title"),
            getResource("ClipWithFeatures.clipFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polygon));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(clipFeatures.key, clipFeatures);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ClipWithFeatures.result.title"),
            getResource("ClipWithFeatures.result.description"));

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
