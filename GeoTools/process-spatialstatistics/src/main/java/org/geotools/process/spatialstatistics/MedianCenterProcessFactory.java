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
 * MedianCenterProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MedianCenterProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(MedianCenterProcessFactory.class);

    private static final String PROCESS_NAME = "MedianCenter";

    /*
     * MedianCenter(SimpleFeatureCollection inputFeatures, String weightField, String caseField, String attributeFields): SimpleFeatureCollection
     */

    public MedianCenterProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new MedianCenterProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("MedianCenter.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("MedianCenter.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("MedianCenter.inputFeatures.title"),
            getResource("MedianCenter.inputFeatures.description"), true, 1, 1, null, null);

    /** weightField */
    public static final Parameter<String> weightField = new Parameter<String>("weightField",
            String.class, getResource("MedianCenter.weightField.title"),
            getResource("MedianCenter.weightField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** caseField */
    public static final Parameter<String> caseField = new Parameter<String>("caseField",
            String.class, getResource("MedianCenter.caseField.title"),
            getResource("MedianCenter.caseField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** attributeFields */
    public static final Parameter<String> attributeFields = new Parameter<String>(
            "attributeFields", String.class, getResource("MedianCenter.attributeFields.title"),
            getResource("MedianCenter.attributeFields.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(weightField.key, weightField);
        parameterInfo.put(caseField.key, caseField);
        parameterInfo.put(attributeFields.key, attributeFields);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("MedianCenter.result.title"),
            getResource("MedianCenter.result.description"));

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
