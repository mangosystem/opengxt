/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.AutoCorrelationMethod;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * LocalSAOverlayProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalSAOverlayProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(LocalSAOverlayProcessFactory.class);

    private static final String PROCESS_NAME = "LocalSAOverlay";

    /*
     * LocalSAOverlay(SimpleFeatureCollection polygonFeatures, SimpleFeatureCollection pointFeatures, Expression weight, AutoCorrelationMethod
     * saMethod, SpatialConcept spatialConcept, DistanceMethod distanceMethod, StandardizationMethod standardization, Double searchDistance):
     * SimpleFeatureCollection
     */

    public LocalSAOverlayProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new LocalSAOverlayProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("LocalSAOverlay.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("LocalSAOverlay.description");
    }

    /** polygonFeatures */
    public static final Parameter<SimpleFeatureCollection> polygonFeatures = new Parameter<SimpleFeatureCollection>(
            "polygonFeatures", SimpleFeatureCollection.class,
            getResource("LocalSAOverlay.polygonFeatures.title"),
            getResource("LocalSAOverlay.polygonFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polygon));

    /** pointFeatures */
    public static final Parameter<SimpleFeatureCollection> pointFeatures = new Parameter<SimpleFeatureCollection>(
            "pointFeatures", SimpleFeatureCollection.class,
            getResource("LocalSAOverlay.pointFeatures.title"),
            getResource("LocalSAOverlay.pointFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Point));

    /** weight */
    public static final Parameter<Expression> weight = new Parameter<Expression>("weight",
            Expression.class, getResource("LocalSAOverlay.weight.title"),
            getResource("LocalSAOverlay.weight.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "features.Number"));

    /** saMethod */
    public static final Parameter<AutoCorrelationMethod> saMethod = new Parameter<AutoCorrelationMethod>(
            "saMethod", AutoCorrelationMethod.class, getResource("LocalSAOverlay.saMethod.title"),
            getResource("LocalSAOverlay.saMethod.description"), false, 0, 1,
            AutoCorrelationMethod.MoranI, null);

    /** spatialConcept */
    public static final Parameter<SpatialConcept> spatialConcept = new Parameter<SpatialConcept>(
            "spatialConcept", SpatialConcept.class,
            getResource("LocalSAOverlay.spatialConcept.title"),
            getResource("LocalSAOverlay.spatialConcept.description"), false, 0, 1,
            SpatialConcept.InverseDistance, null);

    /** distanceMethod */
    public static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod", DistanceMethod.class,
            getResource("LocalSAOverlay.distanceMethod.title"),
            getResource("LocalSAOverlay.distanceMethod.description"), false, 0, 1,
            DistanceMethod.Euclidean, null);

    /** standardization */
    public static final Parameter<StandardizationMethod> standardization = new Parameter<StandardizationMethod>(
            "standardization", StandardizationMethod.class,
            getResource("LocalSAOverlay.standardization.title"),
            getResource("LocalSAOverlay.standardization.description"), false, 0, 1,
            StandardizationMethod.Row, null);

    /** searchDistance */
    public static final Parameter<Double> searchDistance = new Parameter<Double>("searchDistance",
            Double.class, getResource("LocalSAOverlay.searchDistance.title"),
            getResource("LocalSAOverlay.searchDistance.description"), false, 0, 1,
            Double.valueOf(0.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(polygonFeatures.key, polygonFeatures);
        parameterInfo.put(pointFeatures.key, pointFeatures);
        parameterInfo.put(weight.key, weight);
        parameterInfo.put(saMethod.key, saMethod);
        parameterInfo.put(spatialConcept.key, spatialConcept);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(standardization.key, standardization);
        parameterInfo.put(searchDistance.key, searchDistance);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("LocalSAOverlay.result.title"),
            getResource("LocalSAOverlay.result.description"));

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
