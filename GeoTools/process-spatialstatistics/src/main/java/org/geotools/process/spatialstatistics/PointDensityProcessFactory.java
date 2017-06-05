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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * PointDensityProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointDensityProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(PointDensityProcessFactory.class);

    private static final String PROCESS_NAME = "PointDensity";

    /*
     * PointDensity(SimpleFeatureCollection inputFeatures, String populationField, String neighborhood, Double cellSize, ReferencedEnvelope extent):
     * GridCoverage2D
     */

    public PointDensityProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new PointDensityProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("PointDensity.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("PointDensity.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("PointDensity.inputFeatures.title"),
            getResource("PointDensity.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** populationField */
    public static final Parameter<String> populationField = new Parameter<String>(
            "populationField", String.class, getResource("PointDensity.populationField.title"),
            getResource("PointDensity.populationField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** neighborhood */
    public static final Parameter<String> neighborhood = new Parameter<String>("neighborhood",
            String.class, getResource("PointDensity.neighborhood.title"),
            getResource("PointDensity.neighborhood.description"), false, 0, 1, null, null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("PointDensity.cellSize.title"),
            getResource("PointDensity.cellSize.description"), false, 0, 1, Double.valueOf(0.0),
            null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("PointDensity.extent.title"),
            getResource("PointDensity.extent.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(populationField.key, populationField);
        parameterInfo.put(neighborhood.key, neighborhood);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(extent.key, extent);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("PointDensity.result.title"),
            getResource("PointDensity.result.description"));

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
