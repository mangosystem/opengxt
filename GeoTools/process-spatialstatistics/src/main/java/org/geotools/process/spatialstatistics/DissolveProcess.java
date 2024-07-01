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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.DissolveOperation;
import org.geotools.util.logging.Logging;

/**
 * Dissolves features based on specified attributes and aggregation functions.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DissolveProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(DissolveProcess.class);

    public DissolveProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String dissolveField, String statisticsFields, Boolean useMultiPart,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DissolveProcessFactory.inputFeatures.key, inputFeatures);
        map.put(DissolveProcessFactory.dissolveField.key, dissolveField);
        map.put(DissolveProcessFactory.statisticsFields.key, statisticsFields);
        map.put(DissolveProcessFactory.useMultiPart.key, useMultiPart);

        Process process = new DissolveProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(DissolveProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                DissolveProcessFactory.inputFeatures, null);
        String dissolveField = (String) Params.getValue(input,
                DissolveProcessFactory.dissolveField, null);
        if (inputFeatures == null || dissolveField == null) {
            throw new NullPointerException("inputFeatures, dissolveField parameters required");
        }

        String statisticsFields = (String) Params.getValue(input,
                DissolveProcessFactory.statisticsFields, null);
        Boolean useMultiPart = (Boolean) Params.getValue(input,
                DissolveProcessFactory.useMultiPart, DissolveProcessFactory.useMultiPart.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            DissolveOperation operation = new DissolveOperation();
            operation.setUseMultiPart(useMultiPart);
            resultFc = operation.execute(inputFeatures, dissolveField, statisticsFields);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(DissolveProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
