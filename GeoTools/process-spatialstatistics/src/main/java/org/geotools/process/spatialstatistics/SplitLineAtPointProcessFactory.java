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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * SplitLineAtPointProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitLineAtPointProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SplitLineAtPointProcessFactory.class);

    private static final String PROCESS_NAME = "SplitLineAtPoint";

    /*
     * SplitLineAtPoint(SimpleFeatureCollection lineFeatures, SimpleFeatureCollection pointFeatures, Double tolerance): SimpleFeatureCollection
     */

    public SplitLineAtPointProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new SplitLineAtPointProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SplitLineAtPoint.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SplitLineAtPoint.description");
    }

    /** lineFeatures */
    public static final Parameter<SimpleFeatureCollection> lineFeatures = new Parameter<SimpleFeatureCollection>(
            "lineFeatures", SimpleFeatureCollection.class,
            getResource("SplitLineAtPoint.lineFeatures.title"),
            getResource("SplitLineAtPoint.lineFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.LineString));

    /** pointFeatures */
    public static final Parameter<SimpleFeatureCollection> pointFeatures = new Parameter<SimpleFeatureCollection>(
            "pointFeatures", SimpleFeatureCollection.class,
            getResource("SplitLineAtPoint.pointFeatures.title"),
            getResource("SplitLineAtPoint.pointFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** tolerance */
    public static final Parameter<Double> tolerance = new Parameter<Double>("tolerance",
            Double.class, getResource("SplitLineAtPoint.tolerance.title"),
            getResource("SplitLineAtPoint.tolerance.description"), false, 0, 1, Double.valueOf(0d),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(lineFeatures.key, lineFeatures);
        parameterInfo.put(pointFeatures.key, pointFeatures);
        parameterInfo.put(tolerance.key, tolerance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("SplitLineAtPoint.result.title"),
            getResource("SplitLineAtPoint.result.description"), true, 1, 1, null, null);

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
