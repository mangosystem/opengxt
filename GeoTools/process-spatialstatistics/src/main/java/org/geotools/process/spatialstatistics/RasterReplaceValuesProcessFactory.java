/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.locationtech.jts.geom.Geometry;

/**
 * RasterReplaceValuesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReplaceValuesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterReplaceValuesProcessFactory.class);

    private static final String PROCESS_NAME = "RasterReplaceValues";

    /*
     * RasterReplaceValues(GridCoverage2D inputCoverage, Geometry region, Double replaceValue): GridCoverage2D
     */

    public RasterReplaceValuesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterReplaceValuesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterReplaceValues.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterReplaceValues.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterReplaceValues.inputCoverage.title"),
            getResource("RasterReplaceValues.inputCoverage.description"), true, 1, 1, null, null);

    /** region */
    public static final Parameter<Geometry> region = new Parameter<Geometry>("region",
            Geometry.class, getResource("RasterReplaceValues.region.title"),
            getResource("RasterReplaceValues.region.description"), true, 1, 1, null, null);

    /** baseHeight */
    public static final Parameter<Double> replaceValue = new Parameter<Double>("replaceValue",
            Double.class, getResource("RasterReplaceValues.replaceValue.title"),
            getResource("RasterReplaceValues.replaceValue.description"), true, 0, 1,
            Double.valueOf(0d), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(region.key, region);
        parameterInfo.put(replaceValue.key, replaceValue);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterReplaceValues.result.title"),
            getResource("RasterReplaceValues.result.description"));

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
