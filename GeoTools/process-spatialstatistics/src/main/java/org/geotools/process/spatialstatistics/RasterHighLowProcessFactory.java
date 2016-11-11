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
import org.geotools.process.spatialstatistics.gridcoverage.RasterHighLowPointsOperation.HighLowType;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * RasterHighLowPointsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHighLowProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterHighLowProcessFactory.class);

    private static final String PROCESS_NAME = "RasterHighLowPoints";

    /*
     * RasterHighLowPoints(GridCoverage2D inputCoverage, Integer bandIndex, Geometry cropShape, HighLowType valueType): SimpleFeatureCollection
     */

    public RasterHighLowProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterHighLowProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterHighLowPoints.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterHighLowPoints.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterHighLowPoints.inputCoverage.title"),
            getResource("RasterHighLowPoints.inputCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("RasterHighLowPoints.bandIndex.title"),
            getResource("RasterHighLowPoints.bandIndex.description"), false, 0, 1,
            Integer.valueOf(0), null);

    /** cropShape */
    public static final Parameter<Geometry> cropShape = new Parameter<Geometry>("cropShape",
            Geometry.class, getResource("RasterHighLowPoints.cropShape.title"),
            getResource("RasterHighLowPoints.cropShape.description"), false, 0, 1, null, null);

    /** valueType */
    public static final Parameter<HighLowType> valueType = new Parameter<HighLowType>("valueType",
            HighLowType.class, getResource("RasterHighLowPoints.valueType.title"),
            getResource("RasterHighLowPoints.valueType.description"), true, 1, 1, HighLowType.High,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(cropShape.key, cropShape);
        parameterInfo.put(valueType.key, valueType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("RasterHighLowPoints.result.title"),
            getResource("RasterHighLowPoints.result.description"));

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
