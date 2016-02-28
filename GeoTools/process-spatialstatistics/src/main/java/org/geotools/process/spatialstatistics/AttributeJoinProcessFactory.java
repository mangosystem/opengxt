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

import org.geotools.data.Join;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * AttributeJoinProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AttributeJoinProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(AttributeJoinProcessFactory.class);

    private static final String PROCESS_NAME = "AttributeJoin";

    /*
     * AttributeJoin(SimpleFeatureCollection inputFeatures, String primaryKey, SimpleFeatureCollection joinFeatures, String foreignKey, Join.Type
     * joinType) : SimpleFeatureCollection
     */

    public AttributeJoinProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new AttributeJoinProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("AttributeJoin.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("AttributeJoin.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("AttributeJoin.inputFeatures.title"),
            getResource("AttributeJoin.inputFeatures.description"), true, 1, 1, null, null);

    /** primaryKey */
    public static final Parameter<String> primaryKey = new Parameter<String>("primaryKey",
            String.class, getResource("AttributeJoin.primaryKey.title"),
            getResource("AttributeJoin.primaryKey.description"), false, 0, 1, null, new KVP(
                    Parameter.OPTIONS, "inputFeatures.All"));

    /** joinFeatures */
    public static final Parameter<SimpleFeatureCollection> joinFeatures = new Parameter<SimpleFeatureCollection>(
            "joinFeatures", SimpleFeatureCollection.class,
            getResource("AttributeJoin.joinFeatures.title"),
            getResource("AttributeJoin.joinFeatures.description"), true, 1, 1, null, null);

    /** foreignKey */
    public static final Parameter<String> foreignKey = new Parameter<String>("foreignKey",
            String.class, getResource("AttributeJoin.foreignKey.title"),
            getResource("AttributeJoin.foreignKey.description"), false, 0, 1, null, new KVP(
                    Parameter.OPTIONS, "joinFeatures.All"));

    /** joinType */
    public static final Parameter<Join.Type> joinType = new Parameter<Join.Type>("joinType",
            Join.Type.class, getResource("AttributeJoin.joinType.title"),
            getResource("AttributeJoin.joinType.description"), false, 0, 1, Join.Type.INNER, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(primaryKey.key, primaryKey);
        parameterInfo.put(joinFeatures.key, joinFeatures);
        parameterInfo.put(foreignKey.key, foreignKey);
        parameterInfo.put(joinType.key, joinType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("AttributeJoin.result.title"),
            getResource("AttributeJoin.result.description"));

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
