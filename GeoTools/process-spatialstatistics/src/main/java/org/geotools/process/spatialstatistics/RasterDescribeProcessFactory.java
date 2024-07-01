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
import org.geotools.process.spatialstatistics.gridcoverage.RasterDescribeOperation.RasterDescribeResult;
import org.geotools.util.logging.Logging;

/**
 * RasterDescribeProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterDescribeProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterDescribeProcessFactory.class);

    private static final String PROCESS_NAME = "RasterDescribe";

    /*
     * RasterDescribe(GridCoverage2D inputCoverage, Boolean detailed): RasterDescribeResult
     */

    public RasterDescribeProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterDescribeProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterDescribe.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterDescribe.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterDescribe.inputCoverage.title"),
            getResource("RasterDescribe.inputCoverage.description"), true, 1, 1, null, null);

    /** detailed */
    public static final Parameter<Boolean> detailed = new Parameter<Boolean>("detailed",
            Boolean.class, getResource("RasterDescribe.detailed.title"),
            getResource("RasterDescribe.detailed.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(detailed.key, detailed);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<RasterDescribeResult> RESULT = new Parameter<RasterDescribeResult>(
            "result", RasterDescribeResult.class, getResource("RasterDescribe.result.title"),
            getResource("RasterDescribe.result.description"));

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
