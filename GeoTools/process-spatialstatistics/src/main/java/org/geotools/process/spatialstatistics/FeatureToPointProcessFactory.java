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
import org.geotools.util.logging.Logging;

/**
 * FeatureToPointProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureToPointProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FeatureToPointProcessFactory.class);

    private static final String PROCESS_NAME = "FeatureToPoint";

    /*
     * FeatureToPoint(SimpleFeatureCollection inputFeatures, Boolean inside, Boolean singlePart): SimpleFeatureCollection
     */

    public FeatureToPointProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FeatureToPointProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FeatureToPoint.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FeatureToPoint.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("FeatureToPoint.inputFeatures.title"),
            getResource("FeatureToPoint.inputFeatures.description"), true, 1, 1, null, null);

    /** inside */
    public static final Parameter<Boolean> inside = new Parameter<Boolean>("inside", Boolean.class,
            getResource("FeatureToPoint.inside.title"),
            getResource("FeatureToPoint.inside.description"), false, 0, 1, Boolean.TRUE, null);

    /** singlePart */
    public static final Parameter<Boolean> singlePart = new Parameter<Boolean>("singlePart",
            Boolean.class, getResource("FeatureToPoint.singlePart.title"),
            getResource("FeatureToPoint.singlePart.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(inside.key, inside);
        parameterInfo.put(singlePart.key, singlePart);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FeatureToPoint.result.title"),
            getResource("FeatureToPoint.result.description"));

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
