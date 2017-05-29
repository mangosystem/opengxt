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
import org.geotools.process.spatialstatistics.enumeration.ZonalStatisticsType;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * RasterZonalStatisticsProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterZonalStatisticsProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterZonalStatisticsProcessFactory.class);

    private static final String PROCESS_NAME = "ZonalStatistics";

    /*
     * ZonalStatistics(SimpleFeatureCollection zoneFeatures, String targetField, GridCoverage2D valueCoverage, Integer bandIndex, ZonalStaticsType
     * staticsType): SimpleFeatureCollection
     */

    public RasterZonalStatisticsProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterZonalStatisticsProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("ZonalStatistics.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("ZonalStatistics.description");
    }

    /** zoneFeatures */
    public static final Parameter<SimpleFeatureCollection> zoneFeatures = new Parameter<SimpleFeatureCollection>(
            "zoneFeatures", SimpleFeatureCollection.class,
            getResource("ZonalStatistics.zoneFeatures.title"),
            getResource("ZonalStatistics.zoneFeatures.description"), true, 1, 1, null, null);

    /** targetField */
    public static final Parameter<String> targetField = new Parameter<String>("targetField",
            String.class, getResource("ZonalStatistics.targetField.title"),
            getResource("ZonalStatistics.targetField.description"), false, 0, 1, "val", new KVP(
                    Params.FIELD, "zoneFeatures.Number"));

    /** valueCoverage */
    public static final Parameter<GridCoverage2D> valueCoverage = new Parameter<GridCoverage2D>(
            "valueCoverage", GridCoverage2D.class,
            getResource("ZonalStatistics.valueCoverage.title"),
            getResource("ZonalStatistics.valueCoverage.description"), true, 1, 1, null, null);

    /** bandIndex */
    public static final Parameter<Integer> bandIndex = new Parameter<Integer>("bandIndex",
            Integer.class, getResource("ZonalStatistics.bandIndex.title"),
            getResource("ZonalStatistics.bandIndex.description"), false, 0, 1, Integer.valueOf(0),
            null);

    /** statisticsType */
    public static final Parameter<ZonalStatisticsType> statisticsType = new Parameter<ZonalStatisticsType>(
            "statisticsType", ZonalStatisticsType.class,
            getResource("ZonalStatistics.statisticsType.title"),
            getResource("ZonalStatistics.statisticsType.description"), false, 0, 1,
            ZonalStatisticsType.Mean, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(zoneFeatures.key, zoneFeatures);
        parameterInfo.put(targetField.key, targetField);
        parameterInfo.put(valueCoverage.key, valueCoverage);
        parameterInfo.put(bandIndex.key, bandIndex);
        parameterInfo.put(statisticsType.key, statisticsType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("ZonalStatistics.result.title"),
            getResource("ZonalStatistics.result.description"));

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
