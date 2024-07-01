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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;

/**
 * RasterReclassProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReclassProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterReclassProcessFactory.class);

    private static final String PROCESS_NAME = "RasterReclass";

    /*
     * RasterReclass(GridCoverage2D inputCoverage, Integer bandIndex, String ranges, Boolean retainMissingValues): GridCoverage2D
     */

    public RasterReclassProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterReclassProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterReclass.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterReclass.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterReclass.inputCoverage.title"),
            getResource("RasterReclass.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterReclass.bandIndex.title"),
            getResource("RasterReclass.bandIndex.description"), false, 0, 1, Integer.valueOf(0),
            null);

    /** ranges */
    public static final Parameter<String> ranges = new Parameter<String>("ranges", String.class,
            getResource("RasterReclass.ranges.title"),
            getResource("RasterReclass.ranges.description"), true, 1, 1, null, null);

    /** retainMissingValues */
    public static final Parameter<Boolean> retainMissingValues = new Parameter<Boolean>(
            "retainMissingValues", Boolean.class,
            getResource("RasterReclass.retainMissingValues.title"),
            getResource("RasterReclass.retainMissingValues.description"), false, 0, 1,
            Boolean.TRUE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(ranges.key, ranges);
        parameterInfo.put(retainMissingValues.key, retainMissingValues);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterReclass.result.title"),
            getResource("RasterReclass.result.description"));

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
