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
 * RasterShiftProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterShiftProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterShiftProcessFactory.class);

    private static final String PROCESS_NAME = "RasterShift";

    /*
     * RasterShift(GridCoverage2D inputCoverage, Double xShift, Double yShift): GridCoverage2D
     */

    public RasterShiftProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterShiftProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterShift.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterShift.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("RasterShift.inputCoverage.title"),
            getResource("RasterShift.inputCoverage.description"), true, 1, 1, null, null);

    /** xShift */
    public static final Parameter<Double> xShift = new Parameter<Double>("xShift", Double.class,
            getResource("RasterShift.xShift.title"), getResource("RasterShift.xShift.description"),
            false, 0, 1, Double.valueOf(0d), null);

    /** yShift */
    public static final Parameter<Double> yShift = new Parameter<Double>("yShift", Double.class,
            getResource("RasterShift.yShift.title"), getResource("RasterShift.yShift.description"),
            false, 0, 1, Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(xShift.key, xShift);
        parameterInfo.put(yShift.key, yShift);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterShift.result.title"),
            getResource("RasterShift.result.description"));

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
