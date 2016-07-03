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
 * SinglepartToMultipartProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SinglepartToMultipartProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(SinglepartToMultipartProcessFactory.class);

    private static final String PROCESS_NAME = "SinglepartToMultipart";

    /*
     * SinglepartToMultipart(SimpleFeatureCollection inputFeatures, String caseField, Boolean dissolve): SimpleFeatureCollection
     */

    public SinglepartToMultipartProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SinglepartToMultipartProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SinglepartToMultipart.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SinglepartToMultipart.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("SinglepartToMultipart.inputFeatures.title"),
            getResource("SinglepartToMultipart.inputFeatures.description"), true, 1, 1, null, null);

    /** caseField */
    public static final Parameter<String> caseField = new Parameter<String>("caseField",
            String.class, getResource("SinglepartToMultipart.caseField.title"),
            getResource("SinglepartToMultipart.caseField.description"), true, 1, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** dissolve */
    public static final Parameter<Boolean> dissolve = new Parameter<Boolean>("dissolve",
            Boolean.class, getResource("SinglepartToMultipart.dissolve.title"),
            getResource("SinglepartToMultipart.dissolve.description"), false, 0, 1, Boolean.FALSE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(caseField.key, caseField);
        parameterInfo.put(dissolve.key, dissolve);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("SinglepartToMultipart.result.title"),
            getResource("SinglepartToMultipart.result.description"));

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
