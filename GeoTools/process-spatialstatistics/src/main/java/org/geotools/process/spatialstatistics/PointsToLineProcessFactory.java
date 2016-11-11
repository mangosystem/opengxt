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
 * PointsToLineProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsToLineProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(PointsToLineProcessFactory.class);

    private static final String PROCESS_NAME = "PointsToLine";

    /*
     * PointsToLine(SimpleFeatureCollection inputFeatures, String lineField, String sortField, Boolean closeLine): SimpleFeatureCollection
     */

    public PointsToLineProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new PointsToLineProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("PointsToLine.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("PointsToLine.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("PointsToLine.inputFeatures.title"),
            getResource("PointsToLine.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** lineField */
    public static final Parameter<String> lineField = new Parameter<String>("lineField",
            String.class, getResource("PointsToLine.lineField.title"),
            getResource("PointsToLine.lineField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** sortField */
    public static final Parameter<String> sortField = new Parameter<String>("sortField",
            String.class, getResource("PointsToLine.sortField.title"),
            getResource("PointsToLine.sortField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.All"));

    /** closeLine */
    public static final Parameter<Boolean> closeLine = new Parameter<Boolean>("closeLine",
            Boolean.class, getResource("PointsToLine.closeLine.title"),
            getResource("PointsToLine.closeLine.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(lineField.key, lineField);
        parameterInfo.put(sortField.key, sortField);
        parameterInfo.put(closeLine.key, closeLine);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("PointsToLine.result.title"),
            getResource("PointsToLine.result.description"), true, 1, 1, null, null);

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
