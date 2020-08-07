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
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.InternationalString;

/**
 * RasterCutFillProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFillProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillProcessFactory.class);

    private static final String PROCESS_NAME = "RasterCutFill";

    /*
     * RasterCutFill(GridCoverage2D inputDEM, Geometry cropShape, Double baseHeight): SimpleFeatureCollection
     */

    public RasterCutFillProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterCutFillProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterCutFill.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterCutFill.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("RasterCutFill.inputCoverage.title"),
            getResource("RasterCutFill.inputCoverage.description"), true, 1, 1, null, null);

    /** cropShape */
    public static final Parameter<Geometry> cropShape = new Parameter<Geometry>("cropShape",
            Geometry.class, getResource("RasterCutFill.cropShape.title"),
            getResource("RasterCutFill.cropShape.description"), true, 1, 1, null, null);

    /** baseHeight */
    public static final Parameter<Double> baseHeight = new Parameter<Double>("baseHeight",
            Double.class, getResource("RasterCutFill.baseHeight.title"),
            getResource("RasterCutFill.baseHeight.description"), false, 0, 1, Double.valueOf(-9999),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(cropShape.key, cropShape);
        parameterInfo.put(baseHeight.key, baseHeight);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RasterCutFill.result.title"),
            getResource("RasterCutFill.result.description"));

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
