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
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * PolygonsAlongLinesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolygonsAlongLinesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(PolygonsAlongLinesProcessFactory.class);

    private static final String PROCESS_NAME = "PolygonsAlongLines";

    /*
     * PolygonsAlongLines(SimpleFeatureCollection lineFeatures, Expression distance, Expression width, Double mergeFactor): SimpleFeatureCollection
     */

    public PolygonsAlongLinesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new PolygonsAlongLinesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("PolygonsAlongLines.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("PolygonsAlongLines.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("PolygonsAlongLines.lineFeatures.title"),
            getResource("PolygonsAlongLines.lineFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polyline));

    /** distance */
    public static final Parameter<Expression> distance = new Parameter<Expression>("distance",
            Expression.class, getResource("PolygonsAlongLines.distance.title"),
            getResource("PolygonsAlongLines.distance.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** width */
    public static final Parameter<Expression> width = new Parameter<Expression>("width",
            Expression.class, getResource("PolygonsAlongLines.width.title"),
            getResource("PolygonsAlongLines.width.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** quadrantSegments */
    public static final Parameter<Double> mergeFactor = new Parameter<Double>("quadrantSegments",
            Double.class, getResource("PolygonsAlongLines.mergeFactor.title"),
            getResource("PolygonsAlongLines.mergeFactor.description"), false, 0, 1,
            Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(distance.key, distance);
        parameterInfo.put(width.key, width);
        parameterInfo.put(mergeFactor.key, mergeFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("PolygonsAlongLines.result.title"),
            getResource("PolygonsAlongLines.result.description"), true, 1, 1, null, null);

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
