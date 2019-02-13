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
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Point;

/**
 * RotateFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RotateFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RotateFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "RotateFeatures";

    /*
     * RotateFeatures(SimpleFeatureCollection inputFeatures, Point anchor, Expression angle): SimpleFeatureCollection
     */

    public RotateFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RotateFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RotateFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RotateFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("RotateFeatures.inputFeatures.title"),
            getResource("RotateFeatures.inputFeatures.description"), true, 1, 1, null, null);

    /** anchor */
    public static final Parameter<Point> anchor = new Parameter<Point>("anchor", Point.class,
            getResource("RotateFeatures.anchor.title"),
            getResource("RotateFeatures.anchor.description"), false, 0, 1, null, null);

    /** angle */
    public static final Parameter<Expression> angle = new Parameter<Expression>("angle",
            Expression.class, getResource("RotateFeatures.angle.title"),
            getResource("RotateFeatures.angle.description"), true, 1, 1, ff.literal(0d), new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(anchor.key, anchor);
        parameterInfo.put(angle.key, angle);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RotateFeatures.result.title"),
            getResource("RotateFeatures.result.description"));

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
