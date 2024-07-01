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
import org.geotools.process.spatialstatistics.distribution.MeanCenterOperation;
import org.geotools.util.logging.Logging;

/**
 * Identifies the geographic center (or the center of concentration) for a set of features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MeanCenterProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(MeanCenterProcess.class);

    public MeanCenterProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String weightField, String caseField, String dimensionField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MeanCenterProcessFactory.inputFeatures.key, inputFeatures);
        map.put(MeanCenterProcessFactory.weightField.key, weightField);
        map.put(MeanCenterProcessFactory.caseField.key, caseField);
        map.put(MeanCenterProcessFactory.dimensionField.key, dimensionField);

        Process process = new MeanCenterProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(MeanCenterProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                MeanCenterProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        String weightField = (String) Params.getValue(input, MeanCenterProcessFactory.weightField,
                null);
        String caseField = (String) Params
                .getValue(input, MeanCenterProcessFactory.caseField, null);
        String dimensionField = (String) Params.getValue(input,
                MeanCenterProcessFactory.dimensionField, null);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            MeanCenterOperation operation = new MeanCenterOperation();
            resultFc = operation.execute(inputFeatures, weightField, caseField, dimensionField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(MeanCenterProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
