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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.CircularType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * CircularGridProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircularGridProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CircularGridProcessFactory.class);

    private static final String PROCESS_NAME = "CircularGrid";

    /*
     * CircularGrid(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, Double radius, CircularType circularType) :
     * SimpleFeatureCollection
     */

    public CircularGridProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CircularGridProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CircularGrid.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CircularGrid.description");
    }

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("CircularGrid.extent.title"),
            getResource("CircularGrid.extent.description"), true, 1, 1, null, null);

    /** boundsSource */
    public static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("CircularGrid.boundsSource.title"),
            getResource("CircularGrid.boundsSource.description"), false, 0, 1, null, new KVP(
                    Params.FEATURES, "Polygon"));

    /** radius */
    public static final Parameter<Double> radius = new Parameter<Double>("radius", Double.class,
            getResource("CircularGrid.radius.title"),
            getResource("CircularGrid.radius.description"), true, 1, 1, null, null);

    /** circularType */
    public static final Parameter<CircularType> circularType = new Parameter<CircularType>(
            "circularType", CircularType.class, getResource("CircularGrid.circularType.title"),
            getResource("CircularGrid.circularType.description"), false, 0, 1, CircularType.Grid,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(radius.key, radius);
        parameterInfo.put(circularType.key, circularType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("CircularGrid.result.title"),
            getResource("CircularGrid.result.description"));

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
