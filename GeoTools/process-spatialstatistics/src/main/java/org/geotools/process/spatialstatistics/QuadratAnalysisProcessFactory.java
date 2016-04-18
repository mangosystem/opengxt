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
import org.geotools.process.spatialstatistics.pattern.QuadratOperation.QuadratResult;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * QuadratAnalysisProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class QuadratAnalysisProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(QuadratAnalysisProcessFactory.class);

    private static final String PROCESS_NAME = "QuadratAnalysis";

    /*
     * QuadratAnalysis(SimpleFeatureCollection inputFeatures, Double cellSize): QuadratResult
     */

    public QuadratAnalysisProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new QuadratAnalysisProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("QuadratAnalysis.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("QuadratAnalysis.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("QuadratAnalysis.inputFeatures.title"),
            getResource("QuadratAnalysis.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "Point"));

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("QuadratAnalysis.cellSize.title"),
            getResource("QuadratAnalysis.cellSize.description"), false, 0, 1, Double.valueOf(0d),
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(cellSize.key, cellSize);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<QuadratResult> RESULT = new Parameter<QuadratResult>("result",
            QuadratResult.class, getResource("QuadratAnalysis.result.title"),
            getResource("QuadratAnalysis.result.description"));

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
