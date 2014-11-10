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
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * FishnetProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FishnetProcessFactory.class);

    private static final String PROCESS_NAME = "Fishnet";

    /*
     * Fishnet(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, Boolean boundaryInside, Integer columns, Integer rows, Double width,
     * Double height) : SimpleFeatureCollection
     */

    public FishnetProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    protected Process create() {
        return new FishnetProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("Fishnet.title");
    }

    @Override
    protected InternationalString getDescription() {
        return getResource("Fishnet.description");
    }

    /** extent */
    protected static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("Fishnet.extent.title"),
            getResource("Fishnet.extent.description"), false, 0, 1, null, null);

    /** boundsSource */
    protected static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("Fishnet.boundsSource.title"),
            getResource("Fishnet.boundsSource.description"), false, 0, 1, null, null);

    /** boundaryInside */
    public static final Parameter<Boolean> boundaryInside = new Parameter<Boolean>(
            "boundaryInside", Boolean.class, getResource("Fishnet.boundaryInside.title"),
            getResource("Fishnet.boundaryInside.description"), false, 0, 1, Boolean.FALSE, null);

    /** columns */
    protected static final Parameter<Integer> columns = new Parameter<Integer>("columns",
            Integer.class, getResource("Fishnet.columns.title"),
            getResource("Fishnet.columns.description"), false, 0, 1, Integer.valueOf(0), null);

    /** rows */
    protected static final Parameter<Integer> rows = new Parameter<Integer>("rows", Integer.class,
            getResource("Fishnet.rows.title"), getResource("Fishnet.rows.description"), false, 0,
            1, Integer.valueOf(0), null);

    /** width */
    protected static final Parameter<Double> width = new Parameter<Double>("width", Double.class,
            getResource("Fishnet.width.title"), getResource("Fishnet.width.description"), false, 0,
            1, Double.valueOf(0.0), null);

    /** height */
    protected static final Parameter<Double> height = new Parameter<Double>("height", Double.class,
            getResource("Fishnet.height.title"), getResource("Fishnet.height.description"), false,
            0, 1, Double.valueOf(0.0), null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(boundaryInside.key, boundaryInside);
        parameterInfo.put(columns.key, columns);
        parameterInfo.put(rows.key, rows);
        parameterInfo.put(width.key, width);
        parameterInfo.put(height.key, height);
        return parameterInfo;
    }

    /** result */
    protected static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("Fishnet.result.title"),
            getResource("Fishnet.result.description"));

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
