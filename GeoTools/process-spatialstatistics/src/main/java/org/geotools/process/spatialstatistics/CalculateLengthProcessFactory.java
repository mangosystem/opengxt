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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * CalculateLengthProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateLengthProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CalculateLengthProcessFactory.class);

    private static final String PROCESS_NAME = "CalculateLength";

    /*
     * CalculateLength(SimpleFeatureCollection inputFeatures, String lengthField, DistanceUnit lengthUnit): SimpleFeatureCollection
     */

    public CalculateLengthProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CalculateLengthProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CalculateLength.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CalculateLength.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CalculateLength.inputFeatures.title"),
            getResource("CalculateLength.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polyline));

    /** lengthField */
    public static final Parameter<String> lengthField = new Parameter<String>("lengthField",
            String.class, getResource("CalculateLength.lengthField.title"),
            getResource("CalculateLength.lengthField.description"), false, 0, 1, "geom_len",
            new KVP(Params.FIELD, "inputFeatures.All"));

    /** lengthUnit */
    public static final Parameter<DistanceUnit> lengthUnit = new Parameter<DistanceUnit>(
            "lengthUnit", DistanceUnit.class, getResource("CalculateLength.lengthUnit.title"),
            getResource("CalculateLength.lengthUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(lengthField.key, lengthField);
        parameterInfo.put(lengthUnit.key, lengthUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CalculateLength.result.title"),
            getResource("CalculateLength.result.description"));

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
