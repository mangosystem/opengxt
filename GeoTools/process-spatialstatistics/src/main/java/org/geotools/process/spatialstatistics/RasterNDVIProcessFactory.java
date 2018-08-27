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
 * RasterNDVIProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterNDVIProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterNDVIProcessFactory.class);

    private static final String PROCESS_NAME = "RasterNDVI";

    /*
     * RasterNDVI(GridCoverage2D nirCoverage, Integer nirIndex, GridCoverage2D redCoverage, Integer redIndex): GridCoverage2D
     */

    public RasterNDVIProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterNDVIProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterNDVI.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterNDVI.description");
    }

    /** nirCoverage */
    public static final Parameter<GridCoverage2D> nirCoverage = new Parameter<GridCoverage2D>(
            "nirCoverage", GridCoverage2D.class, getResource("RasterNDVI.nirCoverage.title"),
            getResource("RasterNDVI.nirCoverage.description"), true, 1, 1, null, null);

    /** nirIndex */
    public static final Parameter<Integer> nirIndex = new Parameter<Integer>("nirIndex",
            Integer.class, getResource("RasterNDVI.nirIndex.title"),
            getResource("RasterNDVI.nirIndex.description"), true, 1, 1, Integer.valueOf(0), null);

    /** redCoverage */
    public static final Parameter<GridCoverage2D> redCoverage = new Parameter<GridCoverage2D>(
            "redCoverage", GridCoverage2D.class, getResource("RasterNDVI.redCoverage.title"),
            getResource("RasterNDVI.redCoverage.description"), true, 1, 1, null, null);

    /** redIndex */
    public static final Parameter<Integer> redIndex = new Parameter<Integer>("redIndex",
            Integer.class, getResource("RasterNDVI.redIndex.title"),
            getResource("RasterNDVI.redIndex.description"), true, 1, 1, Integer.valueOf(0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(nirCoverage.key, nirCoverage);
        parameterInfo.put(nirIndex.key, nirIndex);
        parameterInfo.put(redCoverage.key, redCoverage);
        parameterInfo.put(redIndex.key, redIndex);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterNDVI.result.title"),
            getResource("RasterNDVI.result.description"));

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
