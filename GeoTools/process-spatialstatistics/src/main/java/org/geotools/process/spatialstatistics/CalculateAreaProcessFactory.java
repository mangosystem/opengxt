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
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * CalculateAreaProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateAreaProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CalculateAreaProcessFactory.class);

    private static final String PROCESS_NAME = "CalculateArea";

    /*
     * CalculateArea(SimpleFeatureCollection inputFeatures, String areaField, AreaUnit areaUnit, String perimeterField, DistanceUnit perimeterUnit):
     * SimpleFeatureCollection
     */

    public CalculateAreaProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CalculateAreaProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CalculateArea.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CalculateArea.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CalculateArea.inputFeatures.title"),
            getResource("CalculateArea.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polygon));

    /** areaField */
    public static final Parameter<String> areaField = new Parameter<String>("areaField",
            String.class, getResource("CalculateArea.areaField.title"),
            getResource("CalculateArea.areaField.description"), false, 0, 1, "geom_area", new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** areaUnit */
    public static final Parameter<AreaUnit> areaUnit = new Parameter<AreaUnit>("areaUnit",
            AreaUnit.class, getResource("CalculateArea.areaUnit.title"),
            getResource("CalculateArea.areaUnit.description"), false, 0, 1, AreaUnit.Default, null);

    /** perimeterField */
    public static final Parameter<String> perimeterField = new Parameter<String>("perimeterField",
            String.class, getResource("CalculateArea.perimeterField.title"),
            getResource("CalculateArea.perimeterField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** perimeterUnit */
    public static final Parameter<DistanceUnit> perimeterUnit = new Parameter<DistanceUnit>(
            "perimeterUnit", DistanceUnit.class, getResource("CalculateArea.perimeterUnit.title"),
            getResource("CalculateArea.perimeterUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(areaField.key, areaField);
        parameterInfo.put(areaUnit.key, areaUnit);
        parameterInfo.put(perimeterField.key, perimeterField);
        parameterInfo.put(perimeterUnit.key, perimeterUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CalculateArea.result.title"),
            getResource("CalculateArea.result.description"));

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
