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

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.StandardizedScoresOperation;
import org.geotools.util.logging.Logging;

/**
 * Calculates a Standardized Score of Dissimilarity.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardizedScoresProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(StandardizedScoresProcess.class);

    public StandardizedScoresProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression xField, Expression yField, String targetField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(StandardizedScoresProcessFactory.inputFeatures.key, inputFeatures);
        map.put(StandardizedScoresProcessFactory.xField.key, xField);
        map.put(StandardizedScoresProcessFactory.yField.key, yField);
        map.put(StandardizedScoresProcessFactory.targetField.key, targetField);

        Process process = new StandardizedScoresProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(StandardizedScoresProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                StandardizedScoresProcessFactory.inputFeatures, null);
        Expression xField = (Expression) Params.getValue(input,
                StandardizedScoresProcessFactory.xField, null);
        Expression yField = (Expression) Params.getValue(input,
                StandardizedScoresProcessFactory.yField, null);
        String targetField = (String) Params.getValue(input,
                StandardizedScoresProcessFactory.targetField,
                StandardizedScoresProcessFactory.targetField.sample);
        if (inputFeatures == null || xField == null || yField == null) {
            throw new NullPointerException("All parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            StandardizedScoresOperation process = new StandardizedScoresOperation();
            resultFc = process.execute(inputFeatures, xField, yField, targetField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(StandardizedScoresProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
