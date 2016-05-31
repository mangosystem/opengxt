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
import org.geotools.process.spatialstatistics.gridcoverage.RasterExtractValuesToPointsOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterExtractValuesToPointsOperation.ExtractionType;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Extracts the cell values of a raster based on a set of point features and records the values in the attribute table of an output feature class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExtractValuesToPointsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ExtractValuesToPointsProcess.class);

    private boolean started = false;

    public ExtractValuesToPointsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection pointFeatures,
            String valueField, GridCoverage2D valueCoverage, ExtractionType valueType,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ExtractValuesToPointsProcessFactory.pointFeatures.key, pointFeatures);
        map.put(ExtractValuesToPointsProcessFactory.valueField.key, valueField);
        map.put(ExtractValuesToPointsProcessFactory.valueCoverage.key, valueCoverage);
        map.put(ExtractValuesToPointsProcessFactory.valueType.key, valueType);

        Process process = new ExtractValuesToPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(ExtractValuesToPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, ExtractValuesToPointsProcessFactory.pointFeatures, null);
            String valueField = (String) Params.getValue(input,
                    ExtractValuesToPointsProcessFactory.valueField,
                    ExtractValuesToPointsProcessFactory.valueField.sample);
            GridCoverage2D valueCoverage = (GridCoverage2D) Params.getValue(input,
                    ExtractValuesToPointsProcessFactory.valueCoverage, null);
            ExtractionType valueType = (ExtractionType) Params.getValue(input,
                    ExtractValuesToPointsProcessFactory.valueType,
                    ExtractValuesToPointsProcessFactory.valueType.sample);

            if (pointFeatures == null || valueField == null || valueCoverage == null) {
                throw new NullPointerException(
                        "pointFeatures, pointFeatures, valueCoverage parameters required");
            }

            // start process
            SimpleFeatureCollection resultSfc = null;
            try {
                RasterExtractValuesToPointsOperation process = new RasterExtractValuesToPointsOperation();
                process.setOutputTypeName(pointFeatures.getSchema().getTypeName());
                resultSfc = process.execute(pointFeatures, valueField, valueCoverage, valueType);
            } catch (Exception ee) {
                monitor.exceptionOccurred(ee);
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(ExtractValuesToPointsProcessFactory.RESULT.key, resultSfc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
