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

import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.operations.RegularPointsOperation.SizeUnit;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * RegularPointsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RegularPointsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(RegularPointsProcessFactory.class);

    private static final String PROCESS_NAME = "RegularPoints";

    /*
     * RegularPoints(ReferencedEnvelope extent, SimpleFeatureCollection boundsSource, SizeUnit unit, Double width, Double height) :
     * SimpleFeatureCollection
     */

    public RegularPointsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RegularPointsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RegularPoints.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RegularPoints.description");
    }

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("RegularPoints.extent.title"),
            getResource("RegularPoints.extent.description"), true, 1, 1, null, null);

    /** boundsSource */
    public static final Parameter<SimpleFeatureCollection> boundsSource = new Parameter<SimpleFeatureCollection>(
            "boundsSource", SimpleFeatureCollection.class,
            getResource("RegularPoints.boundsSource.title"),
            getResource("RegularPoints.boundsSource.description"), false, 0, 1, null, null);

    /** unit */
    public static final Parameter<SizeUnit> unit = new Parameter<SizeUnit>("unit", SizeUnit.class,
            getResource("RegularPoints.unit.title"), getResource("RegularPoints.unit.description"),
            false, 0, 1, SizeUnit.Count, null);

    /** width */
    public static final Parameter<Double> width = new Parameter<Double>("width", Double.class,
            getResource("RegularPoints.width.title"),
            getResource("RegularPoints.width.description"), true, 1, 1, null, null);

    /** height */
    public static final Parameter<Double> height = new Parameter<Double>("height", Double.class,
            getResource("RegularPoints.height.title"),
            getResource("RegularPoints.height.description"), true, 1, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(extent.key, extent);
        parameterInfo.put(boundsSource.key, boundsSource);
        parameterInfo.put(unit.key, unit);
        parameterInfo.put(width.key, width);
        parameterInfo.put(height.key, height);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("RegularPoints.result.title"),
            getResource("RegularPoints.result.description"));

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
