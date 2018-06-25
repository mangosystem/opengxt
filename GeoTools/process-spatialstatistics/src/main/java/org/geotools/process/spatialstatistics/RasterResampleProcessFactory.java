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
import org.opengis.util.InternationalString;

/**
 * RasterResampleProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterResampleProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterResampleProcessFactory.class);

    private static final String PROCESS_NAME = "RasterResample";

    /*
     * RasterResample(GridCoverage2D inputCoverage, Double cellSize, ResampleType resamplingType): GridCoverage2D
     */

    public RasterResampleProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterResampleProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterResample.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterResample.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterResample.inputCoverage.title"),
            getResource("RasterResample.inputCoverage.description"), true, 1, 1, null, null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("RasterResample.cellSize.title"),
            getResource("RasterResample.cellSize.description"), true, 1, 1, Double.valueOf(0.0),
            null);

    /** resamplingType */
    public static final Parameter<ResampleType> resamplingType = new Parameter<ResampleType>(
            "resamplingType", ResampleType.class,
            getResource("RasterResample.resamplingType.title"),
            getResource("RasterResample.resamplingType.description"), false, 0, 1,
            ResampleType.NEAREST, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(resamplingType.key, resamplingType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterResample.result.title"),
            getResource("RasterResample.result.description"));

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
