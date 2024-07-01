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
import org.locationtech.jts.geom.Geometry;

/**
 * RasterCutFill3ProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFill3ProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFill3ProcessFactory.class);

    private static final String PROCESS_NAME = "RasterCutFill3";

    /*
     * RasterCutFill3(GridCoverage2D beforeCoverage, GridCoverage2D afterCoverage, Geometry cropShape, Double baseHeight): SimpleFeatureCollection
     * 
     */

    public RasterCutFill3ProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterCutFill3Process(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterCutFill3.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterCutFill3.description");
    }

    /** beforeCoverage */
    public static final Parameter<GridCoverage2D> beforeCoverage = new Parameter<GridCoverage2D>(
            "beforeCoverage", GridCoverage2D.class,
            getResource("RasterCutFill3.beforeCoverage.title"),
            getResource("RasterCutFill3.beforeCoverage.description"), true, 1, 1, null, null);

    /** afterCoverage */
    public static final Parameter<GridCoverage2D> afterCoverage = new Parameter<GridCoverage2D>(
            "afterCoverage", GridCoverage2D.class,
            getResource("RasterCutFill3.afterCoverage.title"),
            getResource("RasterCutFill3.afterCoverage.description"), true, 1, 1, null, null);

    /** cropShape */
    public static final Parameter<Geometry> cropShape = new Parameter<Geometry>("cropShape",
            Geometry.class, getResource("RasterCutFill3.cropShape.title"),
            getResource("RasterCutFill3.cropShape.description"), true, 1, 1, null, null);

    /** baseHeight */
    public static final Parameter<Double> baseHeight = new Parameter<Double>("baseHeight",
            Double.class, getResource("RasterCutFill.baseHeight.title"),
            getResource("RasterCutFill3.baseHeight.description"), true, 1, 1, Double.valueOf(0),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(beforeCoverage.key, beforeCoverage);
        parameterInfo.put(afterCoverage.key, afterCoverage);
        parameterInfo.put(cropShape.key, cropShape);
        parameterInfo.put(baseHeight.key, baseHeight);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RasterCutFill3.result.title"),
            getResource("RasterCutFill3.result.description"));

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
