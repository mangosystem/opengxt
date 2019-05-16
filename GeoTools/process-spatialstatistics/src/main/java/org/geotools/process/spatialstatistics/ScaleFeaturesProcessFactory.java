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
import org.locationtech.jts.geom.Point;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * ScaleFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ScaleFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ScaleFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "ScaleFeatures";

    /*
     * ScaleFeatures(SimpleFeatureCollection inputFeatures, Expression scaleX, Expression scaleY, Point anchor): SimpleFeatureCollection
     */

    public ScaleFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ScaleFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ScaleFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ScaleFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("ScaleFeatures.inputFeatures.title"),
            getResource("ScaleFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** scaleX */
    public static final Parameter<Expression> scaleX = new Parameter<Expression>("scaleX",
            Expression.class, getResource("ScaleFeatures.scaleX.title"),
            getResource("ScaleFeatures.scaleX.description"), true, 0, 1, ff.literal(0d), new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** scaleY */
    public static final Parameter<Expression> scaleY = new Parameter<Expression>("scaleY",
            Expression.class, getResource("ScaleFeatures.scaleY.title"),
            getResource("ScaleFeatures.scaleY.description"), true, 0, 1, ff.literal(0d), new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** anchor */
    public static final Parameter<Point> anchor = new Parameter<Point>("anchor", Point.class,
            getResource("ScaleFeatures.anchor.title"),
            getResource("ScaleFeatures.anchor.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(scaleX.key, scaleX);
        parameterInfo.put(scaleY.key, scaleY);
        parameterInfo.put(anchor.key, anchor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ScaleFeatures.result.title"),
            getResource("ScaleFeatures.result.description"));

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
