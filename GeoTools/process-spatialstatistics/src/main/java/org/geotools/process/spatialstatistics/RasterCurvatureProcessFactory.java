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
 * RasterCurvatureProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCurvatureProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterCurvatureProcessFactory.class);

    private static final String PROCESS_NAME = "RasterCurvature";

    /*
     * RasterCurvature(GridCoverage2D inputCoverage, Double zFactor): GridCoverage2D
     */

    public RasterCurvatureProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterCurvatureProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterCurvature.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterCurvature.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterCurvature.inputCoverage.title"),
            getResource("RasterCurvature.inputCoverage.description"), true, 1, 1, null, null);

    /** zFactor */
    public static final Parameter<Double> zFactor = new Parameter<Double>("zFactor", Double.class,
            getResource("RasterCurvature.zFactor.title"),
            getResource("RasterCurvature.zFactor.description"), false, 0, 1, Double.valueOf(1.0),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(zFactor.key, zFactor);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterCurvature.result.title"),
            getResource("RasterCurvature.result.description"));

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
