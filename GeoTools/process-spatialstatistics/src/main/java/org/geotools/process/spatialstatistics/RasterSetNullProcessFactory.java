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
import org.geotools.api.filter.Filter;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;

/**
 * RasterSetNullProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSetNullProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterSetNullProcessFactory.class);

    private static final String PROCESS_NAME = "RasterSetNull";

    /*
     * RasterSetNull(GridCoverage2D inputCoverage, Integer bandIndex, Filter filter, Boolean replaceNoData, Double newValue): GridCoverage2D
     */

    public RasterSetNullProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterSetNullProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterSetNull.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterSetNull.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterSetNull.inputCoverage.title"),
            getResource("RasterSetNull.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterSetNull.bandIndex.title"),
            getResource("RasterSetNull.bandIndex.description"), false, 0, 1, Integer.valueOf(0),
            null);

    /** filter */
    public static final Parameter<Filter> filter = new Parameter<Filter>("filter", Filter.class,
            getResource("RasterSetNull.filter.title"),
            getResource("RasterSetNull.filter.description"), true, 1, 1, null, null);

    /** replaceNoData */
    public static final Parameter<Boolean> replaceNoData = new Parameter<Boolean>("replaceNoData",
            Boolean.class, getResource("RasterSetNull.replaceNoData.title"),
            getResource("RasterSetNull.replaceNoData.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** newValue */
    public static final Parameter<Double> newValue = new Parameter<Double>("newValue",
            Double.class, getResource("RasterSetNull.newValue.title"),
            getResource("RasterSetNull.newValue.description"), false, 0, 1, Double.valueOf(0.0d),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(filter.key, filter);
        parameterInfo.put(replaceNoData.key, replaceNoData);
        parameterInfo.put(newValue.key, newValue);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterSetNull.result.title"),
            getResource("RasterSetNull.result.description"));

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
