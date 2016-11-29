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
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * KernelDensityProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KernelDensityProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(KernelDensityProcessFactory.class);

    private static final String PROCESS_NAME = "KernelDensity";

    /*
     * KernelDensity(SimpleFeatureCollection inputFeatures, KernelType kernelType, String populationField, Double searchRadius, Double cellSize,
     * ReferencedEnvelope extent): GridCoverage2D
     */

    public KernelDensityProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new KernelDensityProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("KernelDensity.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("KernelDensity.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("KernelDensity.inputFeatures.title"),
            getResource("KernelDensity.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** kernelType */
    public static final Parameter<KernelType> kernelType = new Parameter<KernelType>("kernelType",
            KernelType.class, getResource("KernelDensity.kernelType.title"),
            getResource("KernelDensity.kernelType.description"), false, 0, 1, KernelType.Quadratic,
            null);

    /** populationField */
    public static final Parameter<String> populationField = new Parameter<String>(
            "populationField", String.class, getResource("KernelDensity.populationField.title"),
            getResource("KernelDensity.populationField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** searchRadius */
    public static final Parameter<Double> searchRadius = new Parameter<Double>("searchRadius",
            Double.class, getResource("KernelDensity.searchRadius.title"),
            getResource("KernelDensity.searchRadius.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("KernelDensity.cellSize.title"),
            getResource("KernelDensity.cellSize.description"), false, 0, 1, Double.valueOf(0.0),
            null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("KernelDensity.extent.title"),
            getResource("KernelDensity.extent.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(kernelType.key, kernelType);
        parameterInfo.put(populationField.key, populationField);
        parameterInfo.put(searchRadius.key, searchRadius);
        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(extent.key, extent);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("KernelDensity.result.title"),
            getResource("KernelDensity.result.description"));

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
