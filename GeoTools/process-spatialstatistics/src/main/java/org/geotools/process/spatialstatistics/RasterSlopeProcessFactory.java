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
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.util.logging.Logging;

/**
 * RasterSlopeProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSlopeProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterSlopeProcessFactory.class);

    private static final String PROCESS_NAME = "RasterSlope";

    /*
     * RasterSlope(GridCoverage2D inputCoverage, SlopeType slopeType, Double zFactor): GridCoverage2D
     */

    public RasterSlopeProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterSlopeProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterSlope.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterSlope.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("RasterSlope.inputCoverage.title"),
            getResource("RasterSlope.inputCoverage.description"), true, 1, 1, null, null);

    /** slopeType */
    public static final Parameter<SlopeType> slopeType = new Parameter<SlopeType>("slopeType",
            SlopeType.class, getResource("RasterSlope.slopeType.title"),
            getResource("RasterSlope.slopeType.description"), false, 0, 1, SlopeType.Degree, null);

    /** zFactor */
    public static final Parameter<Double> zFactor = new Parameter<Double>("zFactor", Double.class,
            getResource("RasterSlope.zFactor.title"),
            getResource("RasterSlope.zFactor.description"), false, 0, 1, Double.valueOf(1.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(slopeType.key, slopeType);
        parameterInfo.put(zFactor.key, zFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterSlope.result.title"),
            getResource("RasterSlope.result.description"));

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
