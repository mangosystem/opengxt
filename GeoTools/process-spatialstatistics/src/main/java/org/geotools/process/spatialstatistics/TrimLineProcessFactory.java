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
 * TrimLineProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TrimLineProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(TrimLineProcessFactory.class);

    private static final String PROCESS_NAME = "TrimLine";

    /*
     * TrimLine(SimpleFeatureCollection lineFeatures, Double dangleLength, Boolean deleteShort): SimpleFeatureCollection
     */

    public TrimLineProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new TrimLineProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("TrimLine.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("TrimLine.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("TrimLine.lineFeatures.title"),
            getResource("TrimLine.lineFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.LineString));

    /** dangleLength */
    public static final Parameter<Double> dangleLength = new Parameter<Double>("dangleLength",
            Double.class, getResource("TrimLine.dangleLength.title"),
            getResource("TrimLine.dangleLength.description"), true, 1, 1, Double.valueOf(0d), null);

    /** deleteShort */
    public static final Parameter<Boolean> deleteShort = new Parameter<Boolean>("deleteShort",
            Boolean.class, getResource("TrimLine.deleteShort.title"),
            getResource("TrimLine.deleteShort.description"), false, 0, 1, Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(dangleLength.key, dangleLength);
        parameterInfo.put(deleteShort.key, deleteShort);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("TrimLine.result.title"),
            getResource("TrimLine.result.description"));

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
