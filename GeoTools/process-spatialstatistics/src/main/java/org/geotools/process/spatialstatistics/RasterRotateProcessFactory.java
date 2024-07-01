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
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Point;

/**
 * RasterRotateProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRotateProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RasterRotateProcessFactory.class);

    private static final String PROCESS_NAME = "RasterRotate";

    /*
     * RasterRotate(GridCoverage2D inputCoverage, Point anchorPoint, Double angle, ResampleType interpolation): GridCoverage2D
     */

    public RasterRotateProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterRotateProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterRotate.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterRotate.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class, getResource("RasterRotate.inputCoverage.title"),
            getResource("RasterRotate.inputCoverage.description"), true, 1, 1, null, null);

    /** anchorPoint */
    public static final Parameter<Point> anchorPoint = new Parameter<Point>("anchorPoint",
            Point.class, getResource("RasterRotate.anchorPoint.title"),
            getResource("RasterRotate.anchorPoint.description"), false, 0, 1, null, null);

    /** angle */
    public static final Parameter<Double> angle = new Parameter<Double>("angle", Double.class,
            getResource("RasterRotate.angle.title"), getResource("RasterRotate.angle.description"),
            true, 1, 1, Double.valueOf(0d), null);

    /** interpolation */
    public static final Parameter<ResampleType> interpolation = new Parameter<ResampleType>(
            "interpolation", ResampleType.class, getResource("RasterRotate.interpolation.title"),
            getResource("RasterRotate.interpolation.description"), false, 0, 1,
            ResampleType.NEAREST, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(anchorPoint.key, anchorPoint);
        parameterInfo.put(angle.key, angle);
        parameterInfo.put(interpolation.key, interpolation);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterRotate.result.title"),
            getResource("RasterRotate.result.description"));

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
