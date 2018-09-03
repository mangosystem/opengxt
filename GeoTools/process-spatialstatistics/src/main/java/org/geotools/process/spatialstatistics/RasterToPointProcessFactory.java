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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * RasterToPointProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToPointProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterToPointProcessFactory.class);

    private static final String PROCESS_NAME = "RasterToPoint";

    /*
     * RasterToPoint(GridCoverage2D inputCoverage, Integer bandIndex, String valueField, Boolean retainNoData): SimpleFeatureCollection
     */

    public RasterToPointProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterToPointProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterToPoint.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterToPoint.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterToPoint.inputCoverage.title"),
            getResource("RasterToPoint.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterToPoint.bandIndex.title"),
            getResource("RasterToPoint.bandIndex.description"), false, 0, 1, Integer.valueOf(0),
            null);

    /** valueField */
    public static final Parameter<String> valueField = new Parameter<String>("valueField",
            String.class, getResource("RasterToPoint.valueField.title"),
            getResource("RasterToPoint.valueField.description"), false, 0, 1, "value", null);

    /** retainNoData */
    public static final Parameter<Boolean> retainNoData = new Parameter<Boolean>("retainNoData",
            Boolean.class, getResource("RasterToPoint.retainNoData.title"),
            getResource("RasterToPoint.retainNoData.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(valueField.key, valueField);
        parameterInfo.put(retainNoData.key, retainNoData);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RasterToPoint.result.title"),
            getResource("RasterToPoint.result.description"));

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
