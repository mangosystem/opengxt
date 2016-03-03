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
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * StatisticsGridCoverageProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsGridCoverageProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(StatisticsGridCoverageProcessFactory.class);

    private static final String PROCESS_NAME = "StatisticsGridCoverage";

    /*
     * StatisticsGridCoverage(GridCoverage2D inputCoverage, Geometry cropShape, Integer bandIndex) : XML
     */

    public StatisticsGridCoverageProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new StatisticsGridCoverageProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("StatisticsGridCoverage.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("StatisticsGridCoverage.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("StatisticsGridCoverage.inputCoverage.title"),
            getResource("StatisticsGridCoverage.inputCoverage.description"), true, 1, 1, null, null);

    /** cropShape */
    public static final Parameter<Geometry> cropShape = new Parameter<Geometry>("cropShape",
            Geometry.class, getResource("StatisticsGridCoverage.cropShape.title"),
            getResource("StatisticsGridCoverage.cropShape.description"), false, 0, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("StatisticsGridCoverage.bandIndex.title"),
            getResource("StatisticsGridCoverage.bandIndex.description"), false, 0, 1,
            Integer.valueOf(0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(cropShape.key, cropShape);
        parameterInfo.put(bandIndex.key, bandIndex);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<DataStatisticsResult> RESULT = new Parameter<DataStatisticsResult>(
            "result", DataStatisticsResult.class,
            getResource("StatisticsGridCoverage.result.title"),
            getResource("StatisticsGridCoverage.result.description"));

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
