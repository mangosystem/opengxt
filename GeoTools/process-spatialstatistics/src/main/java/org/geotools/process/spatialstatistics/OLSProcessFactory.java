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
import org.geotools.process.spatialstatistics.relationship.OLSResult;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * OLSProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OLSProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(OLSProcessFactory.class);

    private static final String PROCESS_NAME = "OrdinaryLeastSquares";

    /*
     * OrdinaryLeastSquares(SimpleFeatureCollection inputFeatures, String dependentVariable, String explanatoryVariables): SimpleFeatureCollection
     */

    public OLSProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new OLSProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("OrdinaryLeastSquares.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("OrdinaryLeastSquares.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("OrdinaryLeastSquares.inputFeatures.title"),
            getResource("OrdinaryLeastSquares.inputFeatures.description"), true, 1, 1, null, null);

    /** dependentVariable */
    public static final Parameter<String> dependentVariable = new Parameter<String>(
            "dependentVariable", String.class,
            getResource("OrdinaryLeastSquares.dependentVariable.title"),
            getResource("OrdinaryLeastSquares.dependentVariable.description"), true, 1, 1, null,
            new KVP(Params.FIELD, "inputFeatures.Number"));

    /** explanatoryVariables */
    public static final Parameter<String> explanatoryVariables = new Parameter<String>(
            "explanatoryVariables", String.class,
            getResource("OrdinaryLeastSquares.explanatoryVariables.title"),
            getResource("OrdinaryLeastSquares.explanatoryVariables.description"), true, 1, 1, null,
            new KVP(Params.FIELDS, "inputFeatures.Number"));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(dependentVariable.key, dependentVariable);
        parameterInfo.put(explanatoryVariables.key, explanatoryVariables);
        return parameterInfo;
    }

    /** report */
    public static final Parameter<OLSResult> report = new Parameter<OLSResult>("report",
            OLSResult.class, getResource("OrdinaryLeastSquares.report.title"),
            getResource("OrdinaryLeastSquares.report.description"), true, 1, 1, null, null);

    /** olsFeatures */
    public static final Parameter<SimpleFeatureCollection> olsFeatures = new Parameter<SimpleFeatureCollection>(
            "olsFeatures", SimpleFeatureCollection.class,
            getResource("OrdinaryLeastSquares.olsFeatures.title"),
            getResource("OrdinaryLeastSquares.olsFeatures.description"), false, 0, 1, null,
            new KVP(Params.STYLES, "OrdinaryLeastSquares.StdResid"));

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(report.key, report);
        resultInfo.put(olsFeatures.key, olsFeatures);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
