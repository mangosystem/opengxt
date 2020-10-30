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
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.InternationalString;

/**
 * AreaProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AreaProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(AreaProcessFactory.class);

    private static final String PROCESS_NAME = "SumAreas";

    /*
     * SumAreas(SimpleFeatureCollection inputFeatures, Filter filter, AreaUnit areaUnit): double
     */

    public AreaProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new AreaProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("SumAreas.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("SumAreas.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("SumAreas.inputFeatures.title"),
            getResource("SumAreas.inputFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polygon));

    /** filter */
    public static final Parameter<Filter> filter = new Parameter<Filter>("filter", Filter.class,
            getResource("SumAreas.filter.title"), getResource("SumAreas.filter.description"), false,
            0, 1, null, null);

    /** areaUnit */
    public static final Parameter<AreaUnit> areaUnit = new Parameter<AreaUnit>("areaUnit",
            AreaUnit.class, getResource("SumAreas.areaUnit.title"),
            getResource("SumAreas.areaUnit.description"), false, 0, 1, AreaUnit.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(filter.key, filter);
        parameterInfo.put(areaUnit.key, areaUnit);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<Double> RESULT = new Parameter<Double>("result", Double.class,
            getResource("SumAreas.result.title"), getResource("SumAreas.result.description"));

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
