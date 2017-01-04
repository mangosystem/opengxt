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

import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * KMeansClusteringProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KMeansClusteringProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(KMeansClusteringProcessFactory.class);

    private static final String PROCESS_NAME = "KMeansClustering";

    // KMeansClustering(SimpleFeatureCollection inputFeatures, String targetField, Integer numberOfClusters, Boolean asCircle):
    // SimpleFeatureCollection

    public KMeansClusteringProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new KMeansClusteringProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("KMeansClustering.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("KMeansClustering.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("KMeansClustering.inputFeatures.title"),
            getResource("KMeansClustering.inputFeatures.description"), true, 1, 1, null, null);

    /** targetField */
    public static final Parameter<String> targetField = new Parameter<String>("targetField",
            String.class, getResource("KMeansClustering.targetField.title"),
            getResource("KMeansClustering.targetField.description"), true, 1, 1, "cluster",
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** numberOfClusters */
    public static final Parameter<Integer> numberOfClusters = new Parameter<Integer>(
            "numberOfClusters", Integer.class,
            getResource("KMeansClustering.numberOfClusters.title"),
            getResource("KMeansClustering.numberOfClusters.description"), true, 1, 1,
            Integer.valueOf(5), null);

    /** asCircle */
    public static final Parameter<Boolean> asCircle = new Parameter<Boolean>("asCircle",
            Boolean.class, getResource("KMeansClustering.asCircle.title"),
            getResource("KMeansClustering.asCircle.description"), false, 0, 1, Boolean.FALSE, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(targetField.key, targetField);
        parameterInfo.put(numberOfClusters.key, numberOfClusters);
        parameterInfo.put(asCircle.key, asCircle);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("KMeansClustering.result.title"),
            getResource("KMeansClustering.result.description"));

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
