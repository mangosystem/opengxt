/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation.FitnessFunctionType;
import org.geotools.process.spatialstatistics.pattern.ClusterBesagNewellOperation;
import org.geotools.util.logging.Logging;

/**
 * Spatial Cluster Detection: Besag, Julian and Newell, James.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClusterBesagNewellProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ClusterBesagNewellProcess.class);

    public ClusterBesagNewellProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static Map<String, Object> process(SimpleFeatureCollection popFeatures,
            Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField,
            Integer neighbours, ProgressListener monitor) {
        return process(popFeatures, popField, caseFeatures, caseField, neighbours,
                (FitnessFunctionType) ClusterBesagNewellProcessFactory.functionType.sample,
                (Double) ClusterBesagNewellProcessFactory.threshold.sample, monitor);
    }

    public static Map<String, Object> process(SimpleFeatureCollection popFeatures,
            Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField,
            Integer neighbours, FitnessFunctionType functionType, Double threshold,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ClusterBesagNewellProcessFactory.popFeatures.key, popFeatures);
        map.put(ClusterBesagNewellProcessFactory.popField.key, popField);
        map.put(ClusterBesagNewellProcessFactory.caseFeatures.key, caseFeatures);
        map.put(ClusterBesagNewellProcessFactory.caseField.key, caseField);
        map.put(ClusterBesagNewellProcessFactory.neighbours.key, neighbours);
        map.put(ClusterBesagNewellProcessFactory.functionType.key, functionType);
        map.put(ClusterBesagNewellProcessFactory.threshold.key, threshold);

        Process process = new ClusterBesagNewellProcess(null);
        Map<String, Object> resultMap = null;
        try {
            resultMap = process.execute(map, monitor);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return resultMap;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection popFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ClusterBesagNewellProcessFactory.popFeatures, null);
        Expression popField = (Expression) Params.getValue(input,
                ClusterBesagNewellProcessFactory.popField, null);
        SimpleFeatureCollection caseFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ClusterBesagNewellProcessFactory.caseFeatures, null);
        Expression caseField = (Expression) Params.getValue(input,
                ClusterBesagNewellProcessFactory.caseField, null);

        if (popFeatures == null || popField == null || caseFeatures == null || caseField == null) {
            throw new NullPointerException("All parameters required");
        }

        Integer neighbours = (Integer) Params.getValue(input,
                ClusterBesagNewellProcessFactory.neighbours,
                ClusterBesagNewellProcessFactory.neighbours.sample);

        FitnessFunctionType functionType = (FitnessFunctionType) Params.getValue(input,
                ClusterBesagNewellProcessFactory.functionType,
                ClusterBesagNewellProcessFactory.functionType.sample);

        Double threshold = (Double) Params.getValue(input,
                ClusterBesagNewellProcessFactory.threshold,
                ClusterBesagNewellProcessFactory.threshold.sample);

        // start process
        SimpleFeatureCollection resultCircles = null;
        GridCoverage2D resultDensity = null;

        ClusterBesagNewellOperation process = new ClusterBesagNewellOperation();
        try {
            process.setFunctionType(functionType);
            process.setThreshold(threshold);

            resultCircles = process.execute(popFeatures, popField, caseFeatures, caseField,
                    neighbours);
            resultDensity = process.getRaster();
        } catch (IOException e) {
            throw new ProcessException(e);
        }

        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ClusterBesagNewellProcessFactory.resultCircles.key, resultCircles);
        resultMap.put(ClusterBesagNewellProcessFactory.resultDensity.key, resultDensity);
        return resultMap;
    }
}
