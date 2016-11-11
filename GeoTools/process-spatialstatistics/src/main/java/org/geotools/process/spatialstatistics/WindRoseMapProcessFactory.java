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

import com.vividsolutions.jts.geom.Geometry;

/**
 * WindRoseMapProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WindRoseMapProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(WindRoseMapProcessFactory.class);

    private static final String PROCESS_NAME = "WindRoseMap";

    /*
     * WindRoseMap(SimpleFeatureCollection inputFeatures, String weightField, Geometry center): SimpleFeatureCollection
     */

    public WindRoseMapProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new WindRoseMapProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("WindRoseMap.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("WindRoseMap.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("WindRoseMap.inputFeatures.title"),
            getResource("WindRoseMap.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Point));

    /** weightField */
    public static final Parameter<String> weightField = new Parameter<String>("weightField",
            String.class, getResource("WindRoseMap.weightField.title"),
            getResource("WindRoseMap.weightField.description"), false, 0, 1, null, new KVP(
                    Params.FIELD, "inputFeatures.Number"));

    /** center */
    public static final Parameter<Geometry> center = new Parameter<Geometry>("center",
            Geometry.class, getResource("WindRoseMap.center.title"),
            getResource("WindRoseMap.center.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(weightField.key, weightField);
        parameterInfo.put(center.key, center);
        return parameterInfo;
    }

    /** windRose */
    public static final Parameter<SimpleFeatureCollection> windRose = new Parameter<SimpleFeatureCollection>(
            "windRose", SimpleFeatureCollection.class, getResource("WindRoseMap.windRose.title"),
            getResource("WindRoseMap.windRose.description"), true, 1, 1, null, new KVP(
                    Params.STYLES, "Quantile.sum"));

    /** anchor */
    public static final Parameter<SimpleFeatureCollection> anchor = new Parameter<SimpleFeatureCollection>(
            "anchor", SimpleFeatureCollection.class, getResource("WindRoseMap.anchor.title"),
            getResource("WindRoseMap.anchor.description"), true, 1, 1, null, null);

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(anchor.key, anchor);
        resultInfo.put(windRose.key, windRose);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
