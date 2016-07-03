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
import org.opengis.filter.Filter;
import org.opengis.util.InternationalString;

/**
 * CountFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CountFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CountFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "CountFeatures";

    /*
     * CountFeatures(SimpleFeatureCollection inputFeatures, Filter filter): Integer
     */

    public CountFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CountFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CountFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CountFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CountFeatures.inputFeatures.title"),
            getResource("CountFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** filter */
    public static final Parameter<Filter> filter = new Parameter<Filter>("filter", Filter.class,
            getResource("CountFeatures.filter.title"),
            getResource("CountFeatures.filter.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(filter.key, filter);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<Integer> RESULT = new Parameter<Integer>("result", Integer.class,
            getResource("CountFeatures.result.title"),
            getResource("CountFeatures.result.description"));

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
