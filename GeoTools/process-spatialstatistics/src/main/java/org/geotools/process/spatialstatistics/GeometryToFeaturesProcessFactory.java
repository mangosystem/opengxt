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
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * GeometryToFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryToFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(GeometryToFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "GeometryToFeatures";

    /*
     * GeometryToFeatures(Geometry geometry, CoordinateReferenceSystem crs, String typeName, Boolean singlePart): SimpleFeatureCollection
     */

    public GeometryToFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new GeometryToFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("GeometryToFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("GeometryToFeatures.description");
    }

    /** inputFeatures */
    public static final Parameter<Geometry> geometry = new Parameter<Geometry>("geometry",
            Geometry.class, getResource("GeometryToFeatures.geometry.title"),
            getResource("GeometryToFeatures.geometry.description"), true, 1, 1, null, null);

    /** crs */
    public static final Parameter<CoordinateReferenceSystem> crs = new Parameter<CoordinateReferenceSystem>(
            "crs", CoordinateReferenceSystem.class, getResource("GeometryToFeatures.crs.title"),
            getResource("GeometryToFeatures.crs.description"), false, 0, 1, null, null);

    /** typeName */
    public static final Parameter<String> typeName = new Parameter<String>("typeName",
            String.class, getResource("GeometryToFeatures.typeName.title"),
            getResource("GeometryToFeatures.typeName.description"), false, 0, 1, "Features", null);

    /** singlePart */
    public static final Parameter<Boolean> singlePart = new Parameter<Boolean>("singlePart",
            Boolean.class, getResource("GeometryToFeatures.singlePart.title"),
            getResource("GeometryToFeatures.singlePart.description"), false, 0, 1, Boolean.FALSE,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(geometry.key, geometry);
        parameterInfo.put(crs.key, crs);
        parameterInfo.put(typeName.key, typeName);
        parameterInfo.put(singlePart.key, singlePart);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("GeometryToFeatures.result.title"),
            getResource("GeometryToFeatures.result.description"));

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
