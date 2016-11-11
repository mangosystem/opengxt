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
import org.geotools.process.spatialstatistics.core.HistogramProcessResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * HistogramGridCoverageProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HistogramGridCoverageProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(HistogramGridCoverageProcessFactory.class);

    private static final String PROCESS_NAME = "HistogramGridCoverage";

    /*
     * HistogramGridCoverage(GridCoverage2D inputCoverage, Geometry cropShape, Integer bandIndex): GridCoverage2D
     */

    public HistogramGridCoverageProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new HistogramGridCoverageProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("HistogramGridCoverage.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("HistogramGridCoverage.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("HistogramGridCoverage.inputCoverage.title"),
            getResource("HistogramGridCoverage.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("HistogramGridCoverage.bandIndex.title"),
            getResource("HistogramGridCoverage.bandIndex.description"), false, 0, 1,
            Integer.valueOf(0), null);

    /** cropShape */
    public static final Parameter<Geometry> cropShape = new Parameter<Geometry>("cropShape",
            Geometry.class, getResource("HistogramGridCoverage.cropShape.title"),
            getResource("HistogramGridCoverage.cropShape.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(cropShape.key, cropShape);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<HistogramProcessResult> RESULT = new Parameter<HistogramProcessResult>(
            "result", HistogramProcessResult.class,
            getResource("HistogramGridCoverage.result.title"),
            getResource("HistogramGridCoverage.result.description"));

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
