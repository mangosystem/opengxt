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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * MultipleRingBufferProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MultipleRingBufferProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(MultipleRingBufferProcessFactory.class);

    private static final String PROCESS_NAME = "MultipleRingBuffer";

    /*
     * MultipleRingBuffer(SimpleFeatureCollection inputFeatures, String distances, DistanceUnit distanceUnit, Boolean outsideOnly, Boolean dissolve):
     * SimpleFeatureCollection
     */

    public MultipleRingBufferProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new MultipleRingBufferProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("MultipleRingBuffer.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("MultipleRingBuffer.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("MultipleRingBuffer.inputFeatures.title"),
            getResource("MultipleRingBuffer.inputFeatures.description"), true, 1, 1, null, null);

    /** distances */
    public static final Parameter<String> distances = new Parameter<String>("distances",
            String.class, getResource("MultipleRingBuffer.distances.title"),
            getResource("MultipleRingBuffer.distances.description"), true, 1, 1, null, null);

    /** distanceUnit */
    public static final Parameter<DistanceUnit> distanceUnit = new Parameter<DistanceUnit>(
            "distanceUnit", DistanceUnit.class,
            getResource("MultipleRingBuffer.distanceUnit.title"),
            getResource("MultipleRingBuffer.distanceUnit.description"), false, 0, 1,
            DistanceUnit.Default, null);

    /** outsideOnly */
    public static final Parameter<Boolean> outsideOnly = new Parameter<Boolean>("outsideOnly",
            Boolean.class, getResource("MultipleRingBuffer.outsideOnly.title"),
            getResource("MultipleRingBuffer.outsideOnly.description"), false, 0, 1, Boolean.TRUE,
            null);

    /** dissolve */
    public static final Parameter<Boolean> dissolve = new Parameter<Boolean>("dissolve",
            Boolean.class, getResource("MultipleRingBuffer.dissolve.title"),
            getResource("MultipleRingBuffer.dissolve.description"), false, 0, 1, Boolean.FALSE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(distances.key, distances);
        parameterInfo.put(distanceUnit.key, distanceUnit);
        parameterInfo.put(outsideOnly.key, outsideOnly);
        parameterInfo.put(dissolve.key, dissolve);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("MultipleRingBuffer.result.title"),
            getResource("MultipleRingBuffer.result.description"));

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
