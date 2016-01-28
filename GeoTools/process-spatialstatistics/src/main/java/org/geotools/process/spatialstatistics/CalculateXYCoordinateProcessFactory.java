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
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * CalculateXYCoordinateProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateXYCoordinateProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(CalculateXYCoordinateProcessFactory.class);

    private static final String PROCESS_NAME = "CalculateXYCoordinate";

    /*
     * CalculateXYCoordinate(SimpleFeatureCollection inputFeatures, String xField, String yField, Boolean inside, CoordinateReferenceSystem targetCRS): SimpleFeatureCollection
     */

    public CalculateXYCoordinateProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new CalculateXYCoordinateProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CalculateXYCoordinate.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("CalculateXYCoordinate.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("CalculateXYCoordinate.inputFeatures.title"),
            getResource("CalculateXYCoordinate.inputFeatures.description"), true, 1, 1, null, null);

    /** xField */
    public static final Parameter<String> xField = new Parameter<String>("xField", String.class,
            getResource("CalculateXYCoordinate.xField.title"),
            getResource("CalculateXYCoordinate.xField.description"), false, 0, 1, "x_coord",
            new KVP(Parameter.OPTIONS, "inputFeatures.All"));

    /** yField */
    public static final Parameter<String> yField = new Parameter<String>("yField", String.class,
            getResource("CalculateXYCoordinate.yField.title"),
            getResource("CalculateXYCoordinate.yField.description"), false, 0, 1, "y_coord",
            new KVP(Parameter.OPTIONS, "inputFeatures.All"));

    /** inside */
    public static final Parameter<Boolean> inside = new Parameter<Boolean>("inside", Boolean.class,
            getResource("CalculateXYCoordinate.inside.title"),
            getResource("CalculateXYCoordinate.inside.description"), false, 0, 1, Boolean.FALSE,
            null);

    /** targetCRS */
    public static final Parameter<CoordinateReferenceSystem> targetCRS = new Parameter<CoordinateReferenceSystem>(
            "targetCRS", CoordinateReferenceSystem.class,
            getResource("CalculateXYCoordinate.targetCRS.title"),
            getResource("CalculateXYCoordinate.targetCRS.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(xField.key, xField);
        parameterInfo.put(yField.key, yField);
        parameterInfo.put(inside.key, inside);
        parameterInfo.put(targetCRS.key, targetCRS);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("CalculateXYCoordinate.result.title"),
            getResource("CalculateXYCoordinate.result.description"));

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
