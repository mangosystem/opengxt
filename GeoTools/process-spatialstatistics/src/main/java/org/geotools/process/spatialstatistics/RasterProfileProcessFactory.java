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
 * RasterProfileProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterProfileProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterProfileProcessFactory.class);

    private static final String PROCESS_NAME = "RasterProfile";

    /*
     * RasterProfile(GridCoverage2D inputCoverage, Geometry userLine, Double interval) : SimpleFeatureCollection
     */

    public RasterProfileProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterProfileProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterProfile.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterProfile.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterProfile.inputCoverage.title"),
            getResource("RasterProfile.inputCoverage.description"), true, 1, 1, null, null);

    /** userLine */
    public static final Parameter<Geometry> userLine = new Parameter<Geometry>("userLine",
            Geometry.class, getResource("RasterProfile.userLine.title"),
            getResource("RasterProfile.userLine.description"), true, 1, 1, null, null);

    /** interval */
    public static final Parameter<Double> interval = new Parameter<Double>("interval",
            Double.class, getResource("RasterProfile.interval.title"),
            getResource("RasterProfile.interval.description"), false, 0, 1, Double.valueOf(20d),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(userLine.key, userLine);
        parameterInfo.put(interval.key, interval);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RasterProfile.result.title"),
            getResource("RasterProfile.result.description"));

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
