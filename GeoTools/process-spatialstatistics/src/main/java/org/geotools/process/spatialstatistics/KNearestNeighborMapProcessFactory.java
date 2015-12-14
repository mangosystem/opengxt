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
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * KNearestNeighborMapProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KNearestNeighborMapProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(KNearestNeighborMapProcessFactory.class);

    private static final String PROCESS_NAME = "KNearestNeighborMap";

    /*
     * KNearestNeighborMap(SimpleFeatureCollection inputFeatures, Integer neighbor, Boolean convexHull): SimpleFeatureCollection
     */

    public KNearestNeighborMapProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new KNearestNeighborMapProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("KNearestNeighborMap.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("KNearestNeighborMap.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("KNearestNeighborMap.inputFeatures.title"),
            getResource("KNearestNeighborMap.inputFeatures.description"), true, 1, 1, null, null);

    /** neighbor */
    public static final Parameter<Integer> neighbor = new Parameter<Integer>("neighbor",
            Integer.class, getResource("KNearestNeighborMap.neighbor.title"),
            getResource("KNearestNeighborMap.neighbor.description"), true, 1, 1,
            Integer.valueOf(1), null);

    /** convexHull */
    public static final Parameter<Boolean> convexHull = new Parameter<Boolean>("convexHull",
            Boolean.class, getResource("KNearestNeighborMap.convexHull.title"),
            getResource("KNearestNeighborMap.convexHull.description"), false, 0, 1, Boolean.TRUE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(neighbor.key, neighbor);
        parameterInfo.put(convexHull.key, convexHull);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("KNearestNeighborMap.result.title"),
            getResource("KNearestNeighborMap.result.description"));

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
