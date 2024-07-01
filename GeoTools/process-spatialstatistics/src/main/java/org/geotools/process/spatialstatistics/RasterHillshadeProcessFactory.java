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
 * RasterHillshadeProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHillshadeProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterHillshadeProcessFactory.class);

    private static final String PROCESS_NAME = "RasterHillshade";

    /*
     * RasterHillshade(GridCoverage2D inputCoverage, Double azimuth, Double altitude, Double zFactor): GridCoverage2D
     */

    public RasterHillshadeProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterHillshadeProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterHillshade.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterHillshade.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterHillshade.inputCoverage.title"),
            getResource("RasterHillshade.inputCoverage.description"), true, 1, 1, null, null);

    /** azimuth */
    public static final Parameter<Double> azimuth = new Parameter<Double>("azimuth", Double.class,
            getResource("RasterHillshade.azimuth.title"),
            getResource("RasterHillshade.azimuth.description"), false, 0, 1, Double.valueOf(315.0),
            null);

    /** altitude */
    public static final Parameter<Double> altitude = new Parameter<Double>("altitude",
            Double.class, getResource("RasterHillshade.altitude.title"),
            getResource("RasterHillshade.altitude.description"), false, 0, 1, Double.valueOf(45.0),
            null);

    /** zFactor */
    public static final Parameter<Double> zFactor = new Parameter<Double>("zFactor", Double.class,
            getResource("RasterSlope.zFactor.title"),
            getResource("RasterSlope.zFactor.description"), false, 0, 1, Double.valueOf(1.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(azimuth.key, azimuth);
        parameterInfo.put(altitude.key, altitude);
        parameterInfo.put(zFactor.key, zFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterHillshade.result.title"),
            getResource("RasterHillshade.result.description"));

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
