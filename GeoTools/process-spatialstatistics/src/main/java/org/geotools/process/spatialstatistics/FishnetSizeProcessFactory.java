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
 * FishnetSizeProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetSizeProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(FishnetSizeProcessFactory.class);

    private static final String PROCESS_NAME = "FishnetSize";

    /*
     * FishnetSize(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, Boolean boundaryInside, Double width, Double height) :
     * SimpleFeatureCollection
     */

    public FishnetSizeProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FishnetSizeProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("FishnetSize.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("FishnetSize.description");
    }

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("FishnetSize.extent.title"),
            getResource("FishnetSize.extent.description"), true, 1, 1, null, null);

    /** boundsSource */
    public static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("FishnetSize.boundsSource.title"),
            getResource("FishnetSize.boundsSource.description"), false, 0, 1, null, null);

    /** boundaryInside */
    public static final Parameter<Boolean> boundaryInside = new Parameter<Boolean>(
            "boundaryInside", Boolean.class, getResource("FishnetSize.boundaryInside.title"),
            getResource("FishnetSize.boundaryInside.description"), false, 0, 1, Boolean.FALSE, null);

    /** width */
    public static final Parameter<Double> width = new Parameter<Double>("width", Double.class,
            getResource("FishnetSize.width.title"), getResource("FishnetSize.width.description"),
            true, 1, 1, null, null);

    /** height */
    public static final Parameter<Double> height = new Parameter<Double>("height", Double.class,
            getResource("FishnetSize.height.title"), getResource("FishnetSize.height.description"),
            true, 1, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(boundaryInside.key, boundaryInside);
        parameterInfo.put(width.key, width);
        parameterInfo.put(height.key, height);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FishnetSize.result.title"),
            getResource("FishnetSize.result.description"));

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
