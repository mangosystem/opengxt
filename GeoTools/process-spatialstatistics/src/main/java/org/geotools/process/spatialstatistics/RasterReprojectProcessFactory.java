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
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * RasterReprojectProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReprojectProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterReprojectProcessFactory.class);

    private static final String PROCESS_NAME = "RasterReproject";

    /*
     * RasterReproject(GridCoverage2D inputCoverage, CoordinateReferenceSystem targetCRS, ResampleType resamplingType, Double cellSize,
     * CoordinateReferenceSystem forcedCRS): GridCoverage2D
     */

    public RasterReprojectProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterReprojectProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterReproject.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterReproject.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterReproject.inputCoverage.title"),
            getResource("RasterReproject.inputCoverage.description"), true, 1, 1, null, null);

    /** targetCRS */
    public static final Parameter<CoordinateReferenceSystem> targetCRS = new Parameter<CoordinateReferenceSystem>(
            "targetCRS", CoordinateReferenceSystem.class,
            getResource("RasterReproject.targetCRS.title"),
            getResource("RasterReproject.targetCRS.description"), true, 1, 1, null, null);

    /** resamplingType */
    public static final Parameter<ResampleType> resamplingType = new Parameter<ResampleType>(
            "resamplingType", ResampleType.class,
            getResource("RasterReproject.resamplingType.title"),
            getResource("RasterReproject.resamplingType.description"), false, 0, 1,
            ResampleType.NEAREST, null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("KernelDensity.cellSize.title"),
            getResource("KernelDensity.cellSize.description"), false, 0, 1, Double.valueOf(0.0),
            null);

    /** forcedCRS */
    public static final Parameter<CoordinateReferenceSystem> forcedCRS = new Parameter<CoordinateReferenceSystem>(
            "forcedCRS", CoordinateReferenceSystem.class,
            getResource("RasterReproject.forcedCRS.title"),
            getResource("RasterReproject.forcedCRS.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(targetCRS.key, targetCRS);
        parameterInfo.put(resamplingType.key, resamplingType);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(forcedCRS.key, forcedCRS);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterReproject.result.title"),
            getResource("RasterReproject.result.description"));

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
