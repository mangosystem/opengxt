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
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * NearestNeighborProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NearestNeighborProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(NearestNeighborProcessFactory.class);

    private static final String PROCESS_NAME = "NearestNeighbor";

    /*
     * NearestNeighbor(SimpleFeatureCollection inputFeatures, DistanceMethod distanceMethod, Double NearestNeighbor): XML
     */

    public NearestNeighborProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    protected Process create() {
        return new NearestNeighborProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("NearestNeighbor.title");
    }

    @Override
    protected InternationalString getDescription() {
        return getResource("NearestNeighbor.description");
    }

    /** inputFeatures */
    protected static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("NearestNeighbor.inputFeatures.title"), getResource("NearestNeighbor.inputFeatures.description"),
            true, 1, 1, null, null);

    /** distanceMethod */
    protected static final Parameter<DistanceMethod> distanceMethod = new Parameter<DistanceMethod>(
            "distanceMethod",
            DistanceMethod.class,
            getResource("NearestNeighbor.distanceMethod.title"), getResource("NearestNeighbor.distanceMethod.description"),
            false, 0, 1, DistanceMethod.Euclidean, null);

    /** area */
    protected static final Parameter<Double> area = new Parameter<Double>("area", Double.class,
            getResource("NearestNeighbor.area.title"), getResource("NearestNeighbor.area.description"),
            false, 0, 1, 0.0, null);
    
    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distanceMethod.key, distanceMethod);
        parameterInfo.put(area.key, area);
        return parameterInfo;
    }

    /** result */
    protected static final Parameter<String> RESULT = new Parameter<String>("result", String.class,
            getResource("NearestNeighbor.result.title"), getResource("NearestNeighbor.result.description"));

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
