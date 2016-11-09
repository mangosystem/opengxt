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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.InternationalString;

/**
 * RasterConProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterConProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterConProcessFactory.class);

    private static final String PROCESS_NAME = "RasterCon";

    /*
     * RasterCon(GridCoverage2D inputCoverage, Integer bandIndex, Filter filter, Integer trueValue, Integer falseValue): GridCoverage2D
     */

    public RasterConProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterConProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterCon.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterCon.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("RasterCon.inputCoverage.title"),
            getResource("RasterCon.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterCon.bandIndex.title"),
            getResource("RasterCon.bandIndex.description"), false, 0, 1, Integer.valueOf(0), null);

    /** filter */
    public static final Parameter<Filter> filter = new Parameter<Filter>("filter", Filter.class,
            getResource("RasterCon.filter.title"), getResource("RasterCon.filter.description"),
            true, 1, 1, null, null);

    /** trueValue */
    public static final Parameter<Integer> trueValue = new Parameter<Integer>("trueValue",
            Integer.class, getResource("RasterCon.trueValue.title"),
            getResource("RasterCon.trueValue.description"), true, 1, 1, Integer.valueOf(1), null);

    /** falseValue */
    public static final Parameter<Integer> falseValue = new Parameter<Integer>("falseValue",
            Integer.class, getResource("RasterCon.falseValue.title"),
            getResource("RasterCon.falseValue.description"), false, 0, 1, Integer.MIN_VALUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(filter.key, filter);
        parameterInfo.put(trueValue.key, trueValue);
        parameterInfo.put(falseValue.key, falseValue);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterCon.result.title"),
            getResource("RasterCon.result.description"));

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
