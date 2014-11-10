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
 * StatisticsFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(StatisticsFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "StatisticsFeatures";

    /*
     * StatisticsFeatures(SimpleFeatureCollection inputFeatures, String inputFields) : XML
     */

    public StatisticsFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    protected Process create() {
        return new StatisticsFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("StatisticsFeatures.title");
    }

    @Override
    protected InternationalString getDescription() {
        return getResource("StatisticsFeatures.description");
    }

    /** inputFeatures */
    protected static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("StatisticsFeatures.inputFeatures.title"),
            getResource("StatisticsFeatures.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "All"));

    /** inputFields */
    public static final Parameter<String> inputFields = new Parameter<String>("inputFields",
            String.class, getResource("StatisticsFeatures.inputFields.title"),
            getResource("StatisticsFeatures.inputFields.description"), true, 1, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(inputFields.key, inputFields);
        return parameterInfo;
    }

    /** result */
    protected static final Parameter<String> RESULT = new Parameter<String>("result", String.class,
            getResource("StatisticsFeatures.result.title"), getResource("StatisticsFeatures.result.description"));

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
