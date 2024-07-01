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

import org.geotools.api.data.Parameter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.InternationalString;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.util.GeometryDimensions.DimensionType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

/**
 * ForceDimensionProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ForceDimensionProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(ForceDimensionProcessFactory.class);

    private static final String PROCESS_NAME = "ForceDimension";

    /*
     * ForceDimension(SimpleFeatureCollection inputFeatures, DimensionType dimension, Expression zField, Expression mField): SimpleFeatureCollection
     */

    public ForceDimensionProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ForceDimensionProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ForceDimension.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ForceDimension.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("ForceDimension.inputFeatures.title"),
            getResource("ForceDimension.inputFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polygon));

    /** dimension */
    public static final Parameter<DimensionType> dimension = new Parameter<DimensionType>(
            "dimension", DimensionType.class, getResource("ForceDimension.dimension.title"),
            getResource("ForceDimension.dimension.description"), true, 1, 1, DimensionType.XY,
            null);

    /** zField */
    public static final Parameter<Expression> zField = new Parameter<Expression>("zField",
            Expression.class, getResource("ForceDimension.zField.title"),
            getResource("ForceDimension.zField.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** mField */
    public static final Parameter<Expression> mField = new Parameter<Expression>("mField",
            Expression.class, getResource("ForceDimension.mField.title"),
            getResource("ForceDimension.mField.description"), false, 0, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(dimension.key, dimension);
        parameterInfo.put(zField.key, zField);
        parameterInfo.put(mField.key, mField);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ForceDimension.result.title"),
            getResource("ForceDimension.result.description"));

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
