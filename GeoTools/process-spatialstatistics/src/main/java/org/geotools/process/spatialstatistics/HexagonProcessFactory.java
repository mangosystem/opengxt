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
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * HexagonProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HexagonProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(HexagonProcessFactory.class);

    private static final String PROCESS_NAME = "Hexagon";

    /*
     * Hexagon(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, Double sideLen, HexagonOrientation orientation) :
     * SimpleFeatureCollection
     */

    public HexagonProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new HexagonProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Hexagon.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("Hexagon.description");
    }

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("Hexagon.extent.title"),
            getResource("Hexagon.extent.description"), true, 1, 1, null, null);

    /** boundsSource */
    public static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("Hexagon.boundsSource.title"),
            getResource("Hexagon.boundsSource.description"), false, 0, 1, null, new KVP(
                    Params.FEATURES, "Polygon"));

    /** sideLen */
    public static final Parameter<Double> sideLen = new Parameter<Double>("sideLen", Double.class,
            getResource("Hexagon.sideLen.title"), getResource("Hexagon.sideLen.description"), true,
            1, 1, null, null);

    /** orientation */
    public static final Parameter<HexagonOrientation> orientation = new Parameter<HexagonOrientation>(
            "orientation", HexagonOrientation.class, getResource("Hexagon.orientation.title"),
            getResource("Hexagon.orientation.description"), false, 0, 1, HexagonOrientation.FLAT,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(sideLen.key, sideLen);
        parameterInfo.put(orientation.key, orientation);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("Hexagon.result.title"),
            getResource("Hexagon.result.description"));

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
