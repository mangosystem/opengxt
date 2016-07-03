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
 * SumLineLengthProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SumLineLengthProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SumLineLengthProcessFactory.class);

    private static final String PROCESS_NAME = "SumLineLength";

    /*
     * SumLineLength(SimpleFeatureCollection polygons, String lengthField, String countField, SimpleFeatureCollection lines): SimpleFeatureCollection
     */

    public SumLineLengthProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SumLineLengthProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SumLineLength.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SumLineLength.description");
    }

    /** polygons */
    public static final Parameter<SimpleFeatureCollection> polygons = new Parameter<SimpleFeatureCollection>(
            "polygons", SimpleFeatureCollection.class, getResource("SumLineLength.polygons.title"),
            getResource("SumLineLength.polygons.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "Polygon"));

    /** lengthField */
    public static final Parameter<String> lengthField = new Parameter<String>("lengthField",
            String.class, getResource("SumLineLength.lengthField.title"),
            getResource("SumLineLength.lengthField.description"), true, 1, 1, "sum_len", new KVP(
                    Params.FIELD, "polygons.All"));

    /** countField */
    public static final Parameter<String> countField = new Parameter<String>("countField",
            String.class, getResource("SumLineLength.countField.title"),
            getResource("SumLineLength.countField.description"), false, 0, 1, "line_cnt", new KVP(
                    Params.FIELD, "polygons.All"));

    /** lines */
    public static final Parameter<SimpleFeatureCollection> lines = new Parameter<SimpleFeatureCollection>(
            "lines", SimpleFeatureCollection.class, getResource("SumLineLength.lines.title"),
            getResource("SumLineLength.lines.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, "LineString"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(polygons.key, polygons);
        parameterInfo.put(lengthField.key, lengthField);
        parameterInfo.put(countField.key, countField);
        parameterInfo.put(lines.key, lines);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("SumLineLength.result.title"),
            getResource("SumLineLength.result.description"), true, 1, 1, null, new KVP(
                    Params.STYLES, "NaturalBreaks.lengthField"));

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
