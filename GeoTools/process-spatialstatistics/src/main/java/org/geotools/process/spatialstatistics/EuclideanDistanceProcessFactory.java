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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;

/**
 * EuclideanDistanceProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class EuclideanDistanceProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(EuclideanDistanceProcessFactory.class);

    private static final String PROCESS_NAME = "EuclideanDistance";

    /*
     * EuclideanDistance(SimpleFeatureCollection inputFeatures, Double maximumDistance, Double cellSize, ReferencedEnvelope extent): GridCoverage2D
     */

    public EuclideanDistanceProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new EuclideanDistanceProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("EuclideanDistance.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("EuclideanDistance.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("EuclideanDistance.inputFeatures.title"),
            getResource("EuclideanDistance.inputFeatures.description"), true, 1, 1, null, null);

    /** maximumDistance */
    public static final Parameter<Double> maximumDistance = new Parameter<Double>(
            "maximumDistance", Double.class,
            getResource("EuclideanDistance.maximumDistance.title"),
            getResource("EuclideanDistance.maximumDistance.description"), false, 0, 1,
            Double.MAX_VALUE, null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("EuclideanDistance.cellSize.title"),
            getResource("EuclideanDistance.cellSize.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("EuclideanDistance.extent.title"),
            getResource("EuclideanDistance.extent.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(maximumDistance.key, maximumDistance);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(extent.key, extent);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("EuclideanDistance.result.title"),
            getResource("EuclideanDistance.result.description"));

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
