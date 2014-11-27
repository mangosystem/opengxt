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
 * CalculateFieldProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateFieldProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CalculateFieldProcessFactory.class);

    private static final String PROCESS_NAME = "CalculateField";

    /*
     * CalculateFieldProcess(SimpleFeatureCollection inputFeatures, String fieldName, String expression): SimpleFeatureCollection
     */

    public CalculateFieldProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CalculateFieldProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CalculateField.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CalculateField.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CalculateField.inputFeatures.title"),
            getResource("CalculateField.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "All"));

    /** fieldName */
    public static final Parameter<String> fieldName = new Parameter<String>("fieldName",
            String.class, getResource("CalculateField.fieldName.title"),
            getResource("CalculateField.fieldName.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "inputFeatures.All"));

    /** expression */
    public static final Parameter<String> expression = new Parameter<String>("expression",
            String.class, getResource("CalculateField.expression.title"),
            getResource("CalculateField.expression.description"), true, 1, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(fieldName.key, fieldName);
        parameterInfo.put(expression.key, expression);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CalculateField.result.title"),
            getResource("CalculateField.result.description"), true, 1, 1, null, new KVP(
                    Parameter.OPTIONS, "NaturalBreaks.fieldName"));

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
