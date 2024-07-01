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
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;

/**
 * FishnetCountProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetCountProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FishnetCountProcessFactory.class);

    private static final String PROCESS_NAME = "FishnetCount";

    /*
     * FishnetCount(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, Boolean boundaryInside, Integer columns, Integer rows ) :
     * SimpleFeatureCollection
     */

    public FishnetCountProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FishnetCountProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FishnetCount.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FishnetCount.description");
    }

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("FishnetCount.extent.title"),
            getResource("FishnetCount.extent.description"), true, 1, 1, null, null);

    /** boundsSource */
    public static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("FishnetCount.boundsSource.title"),
            getResource("FishnetCount.boundsSource.description"), false, 0, 1, null, null);

    /** boundaryInside */
    public static final Parameter<Boolean> boundaryInside = new Parameter<Boolean>(
            "boundaryInside", Boolean.class, getResource("FishnetCount.boundaryInside.title"),
            getResource("FishnetCount.boundaryInside.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** columns */
    public static final Parameter<Integer> columns = new Parameter<Integer>("columns",
            Integer.class, getResource("FishnetCount.columns.title"),
            getResource("FishnetCount.columns.description"), true, 1, 1, null, null);

    /** rows */
    public static final Parameter<Integer> rows = new Parameter<Integer>("rows", Integer.class,
            getResource("FishnetCount.rows.title"), getResource("FishnetCount.rows.description"),
            true, 1, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(boundaryInside.key, boundaryInside);
        parameterInfo.put(columns.key, columns);
        parameterInfo.put(rows.key, rows);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FishnetCount.result.title"),
            getResource("FishnetCount.result.description"));

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
