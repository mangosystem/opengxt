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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.enumeration.RadialType;
import org.geotools.util.logging.Logging;

/**
 * PolarGridsFromFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolarGridsFromFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(PolarGridsFromFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "PolarGridsFromFeatures";

    /*
     * PolarGridsFromFeatures(SimpleFeatureCollection origin, String radius, DistanceUnit radiusUnit, RadialType radialType, Integer sides):
     * SimpleFeatureCollection
     */

    public PolarGridsFromFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new PolarGridsFromFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("PolarGridsFromFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("PolarGridsFromFeatures.description");
    }

    /** origin */
    public static final Parameter<SimpleFeatureCollection> origin = new Parameter<SimpleFeatureCollection>(
            "origin", SimpleFeatureCollection.class,
            getResource("PolarGridsFromFeatures.origin.title"),
            getResource("PolarGridsFromFeatures.origin.description"), true, 1, 1, null, null);

    /** radius */
    public static final Parameter<String> radius = new Parameter<String>("radius", String.class,
            getResource("PolarGridsFromFeatures.radius.title"),
            getResource("PolarGridsFromFeatures.radius.description"), true, 1, 1, null, null);

    /** radiusUnit */
    public static final Parameter<DistanceUnit> radiusUnit = new Parameter<DistanceUnit>(
            "radiusUnit", DistanceUnit.class,
            getResource("PolarGridsFromFeatures.radiusUnit.title"),
            getResource("PolarGridsFromFeatures.radiusUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    /** radialType */
    public static final Parameter<RadialType> radialType = new Parameter<RadialType>("radialType",
            RadialType.class, getResource("PolarGridsFromFeatures.radialType.title"),
            getResource("PolarGridsFromFeatures.radialType.description"), false, 0, 1,
            RadialType.Polar, null);

    /** sides */
    public static final Parameter<Integer> sides = new Parameter<Integer>("sides", Integer.class,
            getResource("PolarGridsFromFeatures.sides.title"),
            getResource("PolarGridsFromFeatures.sides.description"), false, 0, 1,
            Integer.valueOf(8), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(origin.key, origin);
        parameterInfo.put(radius.key, radius);
        parameterInfo.put(radiusUnit.key, radiusUnit);
        parameterInfo.put(radialType.key, radialType);
        parameterInfo.put(sides.key, sides);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("PolarGridsFromFeatures.result.title"),
            getResource("PolarGridsFromFeatures.result.description"), true, 1, 1, null, null);

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