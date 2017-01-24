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
 * IntersectionPointsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IntersectionPointsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(IntersectionPointsProcessFactory.class);

    private static final String PROCESS_NAME = "IntersectionPoints";

    /*
     * IntersectionPoints(SimpleFeatureCollection inputFeatures, SimpleFeatureCollection intersectFeatures, String intersectIDField ):
     * SimpleFeatureCollection
     */

    public IntersectionPointsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new IntersectionPointsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("IntersectionPoints.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("IntersectionPoints.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("IntersectionPoints.inputFeatures.title"),
            getResource("IntersectionPoints.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polyline));

    /** intersectFeatures */
    public static final Parameter<SimpleFeatureCollection> intersectFeatures = new Parameter<SimpleFeatureCollection>(
            "intersectFeatures", SimpleFeatureCollection.class,
            getResource("IntersectionPoints.intersectFeatures.title"),
            getResource("IntersectionPoints.intersectFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polyline));

    /** intersectIDField */
    public static final Parameter<String> intersectIDField = new Parameter<String>(
            "intersectIDField", String.class,
            getResource("IntersectionPoints.intersectIDField.title"),
            getResource("IntersectionPoints.intersectIDField.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "intersectFeatures.All"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(intersectFeatures.key, intersectFeatures);
        parameterInfo.put(intersectIDField.key, intersectIDField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("IntersectionPoints.result.title"),
            getResource("IntersectionPoints.result.description"), true, 1, 1, null, null);

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
