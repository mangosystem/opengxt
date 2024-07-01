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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;

/**
 * RasterToPolygonProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToPolygonProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterToPolygonProcessFactory.class);

    private static final String PROCESS_NAME = "RasterToPolygon";

    /*
     * RasterToPolygon(GridCoverage2D inputCoverage, Integer bandIndex, Boolean weeding, String valueField): SimpleFeatureCollection
     */

    public RasterToPolygonProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterToPolygonProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterToPolygon.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterToPolygon.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterToPolygon.inputCoverage.title"),
            getResource("RasterToPolygon.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterToPolygon.bandIndex.title"),
            getResource("RasterToPolygon.bandIndex.description"), false, 0, 1, Integer.valueOf(0),
            null);

    /** weeding */
    public static final Parameter<Boolean> weeding = new Parameter<Boolean>("weeding",
            Boolean.class, getResource("RasterToPolygon.weeding.title"),
            getResource("RasterToPolygon.weeding.description"), false, 0, 1, Boolean.FALSE, null);

    /** valueField */
    public static final Parameter<String> valueField = new Parameter<String>("valueField",
            String.class, getResource("RasterToPolygon.valueField.title"),
            getResource("RasterToPolygon.valueField.description"), false, 0, 1, "value", null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(weeding.key, weeding);
        parameterInfo.put(valueField.key, valueField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RasterToPolygon.result.title"),
            getResource("RasterToPolygon.result.description"));

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
