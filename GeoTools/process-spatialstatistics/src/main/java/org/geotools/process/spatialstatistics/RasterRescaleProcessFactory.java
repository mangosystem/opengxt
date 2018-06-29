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
import org.opengis.util.InternationalString;

/**
 * RasterRescaleProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRescaleProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterRescaleProcessFactory.class);

    private static final String PROCESS_NAME = "RasterRescale";

    /*
     * RasterRescale(GridCoverage2D inputCoverage, Double xScale, Double yScale): GridCoverage2D
     */

    public RasterRescaleProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterRescaleProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterRescale.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterRescale.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterRescale.inputCoverage.title"),
            getResource("RasterRescale.inputCoverage.description"), true, 1, 1, null, null);

    /** xScale */
    public static final Parameter<Double> xScale = new Parameter<Double>("xScale", Double.class,
            getResource("RasterRescale.xScale.title"),
            getResource("RasterRescale.xScale.description"), false, 0, 1, Double.valueOf(0d), null);

    /** yScale */
    public static final Parameter<Double> yScale = new Parameter<Double>("yScale", Double.class,
            getResource("RasterRescale.yScale.title"),
            getResource("RasterRescale.yScale.description"), false, 0, 1, Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(xScale.key, xScale);
        parameterInfo.put(yScale.key, yScale);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterRescale.result.title"),
            getResource("RasterRescale.result.description"));

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
