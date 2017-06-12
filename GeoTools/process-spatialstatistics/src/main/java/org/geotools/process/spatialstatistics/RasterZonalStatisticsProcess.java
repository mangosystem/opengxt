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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.ZonalStatisticsType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterZonalOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates statistics on values of a raster within the zones of another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterZonalStatisticsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterZonalStatisticsProcess.class);

    public RasterZonalStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection zoneFeatures,
            String targetField, GridCoverage2D inputCoverage, Integer bandIndex,
            ZonalStatisticsType statisticsType, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterZonalStatisticsProcessFactory.zoneFeatures.key, zoneFeatures);
        map.put(RasterZonalStatisticsProcessFactory.targetField.key, targetField);
        map.put(RasterZonalStatisticsProcessFactory.valueCoverage.key, inputCoverage);
        map.put(RasterZonalStatisticsProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterZonalStatisticsProcessFactory.statisticsType.key, statisticsType);

        Process process = new RasterZonalStatisticsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(RasterZonalStatisticsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection zoneFeatures = (SimpleFeatureCollection) Params.getValue(input,
                RasterZonalStatisticsProcessFactory.zoneFeatures, null);
        GridCoverage2D valueCoverage = (GridCoverage2D) Params.getValue(input,
                RasterZonalStatisticsProcessFactory.valueCoverage, null);
        if (valueCoverage == null || zoneFeatures == null) {
            throw new NullPointerException("valueCoverage, zoneFeatures parameters required");
        }

        Integer bandIndex = (Integer) Params.getValue(input,
                RasterZonalStatisticsProcessFactory.bandIndex,
                RasterZonalStatisticsProcessFactory.bandIndex.sample);

        ZonalStatisticsType statisticsType = (ZonalStatisticsType) Params.getValue(input,
                RasterZonalStatisticsProcessFactory.statisticsType,
                RasterZonalStatisticsProcessFactory.statisticsType.sample);

        String targetField = (String) Params.getValue(input,
                RasterZonalStatisticsProcessFactory.targetField,
                RasterZonalStatisticsProcessFactory.targetField.sample);

        if (targetField == null || targetField.isEmpty()) {
            targetField = statisticsType.name();
        }

        // start process
        SimpleFeatureCollection result = null;
        try {
            RasterZonalOperation process = new RasterZonalOperation();
            result = process.execute(zoneFeatures, targetField, valueCoverage, bandIndex,
                    statisticsType);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterZonalStatisticsProcessFactory.RESULT.key, result);
        return resultMap;
    }
}
