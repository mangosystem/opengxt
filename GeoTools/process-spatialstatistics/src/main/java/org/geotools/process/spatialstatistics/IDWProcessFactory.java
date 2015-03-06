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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.gridcoverage.RasterInterpolationOperator.RadiusType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * IDWProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IDWProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(IDWProcessFactory.class);

    private static final String PROCESS_NAME = "IDW";

    /*
     * IDW(SimpleFeatureCollection inputFeatures, String inputField, Double power, RadiusType radiusType, Integer numberOfPoints, Double distance,
     * Double cellSize, ReferencedEnvelope extent): GridCoverage2D
     */

    public IDWProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new IDWProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("IDW.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("IDW.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class, getResource("IDW.inputFeatures.title"),
            getResource("IDW.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Parameter.FEATURE_TYPE, "Point"));

    /** inputField */
    public static final Parameter<String> inputField = new Parameter<String>("inputField",
            String.class, getResource("IDW.inputField.title"),
            getResource("IDW.inputField.description"), false, 0, 1, null, new KVP(
                    Parameter.OPTIONS, "inputFeatures.Number"));

    /** power */
    public static final Parameter<Double> power = new Parameter<Double>("power", Double.class,
            getResource("IDW.power.title"), getResource("IDW.power.description"), false, 0, 1,
            Double.valueOf(2.0), null);

    /** radiusType */
    public static final Parameter<RadiusType> radiusType = new Parameter<RadiusType>("radiusType",
            RadiusType.class, getResource("IDW.radiusType.title"),
            getResource("IDW.radiusType.description"), false, 0, 1, RadiusType.Variable, null);

    /** numberOfPoints */
    public static final Parameter<Integer> numberOfPoints = new Parameter<Integer>(
            "numberOfPoints", Integer.class, getResource("IDW.numberOfPoints.title"),
            getResource("IDW.numberOfPoints.description"), false, 0, 1, Integer.valueOf(12), null);

    /** distance */
    public static final Parameter<Double> distance = new Parameter<Double>("distance",
            Double.class, getResource("IDW.distance.title"),
            getResource("IDW.distance.description"), false, 0, 1, Double.valueOf(0.0), null);

    /** cellSize */
    public static final Parameter<Double> cellSize = new Parameter<Double>("cellSize",
            Double.class, getResource("IDW.cellSize.title"),
            getResource("IDW.cellSize.description"), false, 0, 1, Double.valueOf(0.0), null);

    /** extent */
    public static final Parameter<ReferencedEnvelope> extent = new Parameter<ReferencedEnvelope>(
            "extent", ReferencedEnvelope.class, getResource("IDW.extent.title"),
            getResource("IDW.extent.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(inputField.key, inputField);
        parameterInfo.put(power.key, power);
        parameterInfo.put(radiusType.key, radiusType);
        parameterInfo.put(numberOfPoints.key, numberOfPoints);
        parameterInfo.put(distance.key, distance);

        parameterInfo.put(cellSize.key, cellSize);
        parameterInfo.put(extent.key, extent);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("IDW.result.title"),
            getResource("IDW.result.description"));

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
