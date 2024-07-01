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
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.AutoCorrelationMethod;
import org.geotools.process.spatialstatistics.enumeration.BinningGridType;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * LocalSABinningProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalSABinningProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(LocalSABinningProcessFactory.class);

    private static final String PROCESS_NAME = "LocalSABinning";

    /*
     * LocalSABinning(SimpleFeatureCollection features, Expression weight, BinningGridType gridType, ReferencedEnvelope extent, Double size,
     * AutoCorrelationMethod saMethod, SpatialConcept spatialConcept, DistanceMethod distanceMethod, StandardizationMethod standardization, Double
     * searchDistance): SimpleFeatureCollection
     */

    public LocalSABinningProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new LocalSABinningProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("LocalSABinning.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("LocalSABinning.description");
    }

    /** features */
    public static final Parameter<SimpleFeatureCollection> features = new Parameter<SimpleFeatureCollection>(
            "features", SimpleFeatureCollection.class, getResource("LocalSABinning.features.title"),
            getResource("LocalSABinning.features.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Point));

    /** weight */
    public static final Parameter<Expression> weight = new Parameter<Expression>("weight",
            Expression.class, getResource("LocalSABinning.weight.title"),
            getResource("LocalSABinning.weight.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "features.Number"));

    /** gridType */
    public static final Parameter<BinningGridType> gridType = new Parameter<BinningGridType>(
            "gridType", BinningGridType.class, getResource("LocalSABinning.gridType.title"),
            getResource("LocalSABinning.gridType.description"), false, 0, 1,
            BinningGridType.Hexagon, null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("LocalSABinning.extent.title"),
            getResource("LocalSABinning.extent.description"), false, 0, 1, null, null);

    /** size */
    public static final Parameter<Double> size = new Parameter<Double>("size", Double.class,
            getResource("LocalSABinning.size.title"),
            getResource("LocalSABinning.size.description"), false, 0, 1, Double.valueOf(0.0), null);

    /** saMethod */
    public static final Parameter<AutoCorrelationMethod> saMethod = new Parameter<AutoCorrelationMethod>(
            "saMethod", AutoCorrelationMethod.class, getResource("LocalSABinning.saMethod.title"),
            getResource("LocalSABinning.saMethod.description"), false, 0, 1,
            AutoCorrelationMethod.MoranI, null);

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class,
            getResource("LocalSABinning.spatialConcept.title"),
            getResource("LocalSABinning.spatialConcept.description"), false, 0, 1,
            SpatialConcept.InverseDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("LocalSABinning.distanceMethod.title"),
            getResource("LocalSABinning.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** standardization */
    public static final Parameter<StandardizationMethod> standardization = new Parameter<StandardizationMethod>(
            "standardization", StandardizationMethod.class,
            getResource("LocalSABinning.standardization.title"),
            getResource("LocalSABinning.standardization.description"), false, 0, 1,
            StandardizationMethod.Row, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("LocalSABinning.searchDistance.title"),
            getResource("LocalSABinning.searchDistance.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(features.key, features);
        parameterInfo.put(weight.key, weight);
        parameterInfo.put(gridType.key, gridType);
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(size.key, size);
        parameterInfo.put(saMethod.key, saMethod);
        parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(standardization.key, standardization);
        parameterInfo.put(searchDistance.key, searchDistance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("LocalSABinning.result.title"),
            getResource("LocalSABinning.result.description"));

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
