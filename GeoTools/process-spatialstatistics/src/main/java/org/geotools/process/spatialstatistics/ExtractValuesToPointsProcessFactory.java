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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterExtractValuesToPointsOperation.ExtractionType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * ExtractValuesToPointsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExtractValuesToPointsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(ExtractValuesToPointsProcessFactory.class);

    private static final String PROCESS_NAME = "ExtractValuesToPoints";

    /*
     * ExtractValuesToPoints(SimpleFeatureCollection pointFeatures, String valueField, GridCoverage2D valueCoverage, ExtractionType valueType) :
     * SimpleFeatureCollection
     */

    public ExtractValuesToPointsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new ExtractValuesToPointsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ExtractValuesToPoints.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ExtractValuesToPoints.description");
    }

    /** pointFeatures */
    public static final Parameter<SimpleFeatureCollection> pointFeatures = new Parameter<SimpleFeatureCollection>(
            "pointFeatures", SimpleFeatureCollection.class,
            getResource("ExtractValuesToPoints.pointFeatures.title"),
            getResource("ExtractValuesToPoints.pointFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Point));

    /** valueField */
    public static final Parameter<String> valueField = new Parameter<String>("valueField",
            String.class, getResource("ExtractValuesToPoints.valueField.title"),
            getResource("ExtractValuesToPoints.valueField.description"), false, 0, 1, "rasterval",
            new KVP(Params.FIELD, "pointFeatures.All"));

    /** valueCoverage */
    public static final Parameter<GridCoverage2D> valueCoverage = new Parameter<GridCoverage2D>(
            "valueCoverage", GridCoverage2D.class,
            getResource("ExtractValuesToPoints.valueCoverage.title"),
            getResource("ExtractValuesToPoints.valueCoverage.description"), true, 1, 1, null, null);

    /** valueType */
    public static final Parameter<ExtractionType> valueType = new Parameter<ExtractionType>(
            "valueType", ExtractionType.class,
            getResource("ExtractValuesToPoints.valueType.title"),
            getResource("ExtractValuesToPoints.valueType.description"), false, 0, 1,
            ExtractionType.Default, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(pointFeatures.key, pointFeatures);
        parameterInfo.put(valueField.key, valueField);
        parameterInfo.put(valueCoverage.key, valueCoverage);
        parameterInfo.put(valueType.key, valueType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class,
            getResource("ExtractValuesToPoints.result.title"),
            getResource("ExtractValuesToPoints.result.description"));

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
