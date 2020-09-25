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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation;
import org.geotools.process.spatialstatistics.pattern.ClusterGAMOperation;
import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation.FitnessFunctionType;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Spatial Cluster Detection: Openshaw's Geographical Analysis Machine(GAM).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClusterGAMProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ClusterGAMProcess.class);

    public ClusterGAMProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static Map<String, Object> process(SimpleFeatureCollection popFeatures,
            Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField,
            ProgressListener monitor) {
        return process(popFeatures, popField, caseFeatures, caseField, Double.valueOf(0d),
                Double.valueOf(0d), Double.valueOf(0d), monitor);
    }

    public static Map<String, Object> process(SimpleFeatureCollection popFeatures,
            Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField,
            Double minRadius, Double maxRadius, Double radiusIncrement, ProgressListener monitor) {
        return process(popFeatures, popField, caseFeatures, caseField, minRadius, maxRadius,
                radiusIncrement, (Double) ClusterGAMProcessFactory.overlapRatio.sample,
                (FitnessFunctionType) ClusterGAMProcessFactory.functionType.sample,
                (Double) ClusterGAMProcessFactory.threshold.sample, monitor);
    }

    public static Map<String, Object> process(SimpleFeatureCollection popFeatures,
            Expression popField, SimpleFeatureCollection caseFeatures, Expression caseField,
            Double minRadius, Double maxRadius, Double radiusIncrement, Double overlapRatio,
            FitnessFunctionType functionType, Double threshold, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ClusterGAMProcessFactory.popFeatures.key, popFeatures);
        map.put(ClusterGAMProcessFactory.popField.key, popField);
        map.put(ClusterGAMProcessFactory.caseFeatures.key, caseFeatures);
        map.put(ClusterGAMProcessFactory.caseField.key, caseField);
        map.put(ClusterGAMProcessFactory.minRadius.key, minRadius);
        map.put(ClusterGAMProcessFactory.maxRadius.key, maxRadius);
        map.put(ClusterGAMProcessFactory.radiusIncrement.key, radiusIncrement);
        map.put(ClusterGAMProcessFactory.overlapRatio.key, overlapRatio);
        map.put(ClusterGAMProcessFactory.functionType.key, functionType);
        map.put(ClusterGAMProcessFactory.threshold.key, threshold);

        Process process = new ClusterGAMProcess(null);
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
                ClusterGAMProcessFactory.popFeatures, null);
        Expression popField = (Expression) Params.getValue(input, ClusterGAMProcessFactory.popField,
                null);
        SimpleFeatureCollection caseFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ClusterGAMProcessFactory.caseFeatures, null);
        Expression caseField = (Expression) Params.getValue(input,
                ClusterGAMProcessFactory.caseField, null);

        if (popFeatures == null || popField == null || caseFeatures == null || caseField == null) {
            throw new NullPointerException("All parameters required");
        }

        Double minRadius = (Double) Params.getValue(input, ClusterGAMProcessFactory.minRadius,
                ClusterGAMProcessFactory.minRadius.sample);

        Double maxRadius = (Double) Params.getValue(input, ClusterGAMProcessFactory.maxRadius,
                ClusterGAMProcessFactory.maxRadius.sample);

        Double radiusIncrement = (Double) Params.getValue(input,
                ClusterGAMProcessFactory.radiusIncrement,
                ClusterGAMProcessFactory.radiusIncrement.sample);

        Double overlapRatio = (Double) Params.getValue(input, ClusterGAMProcessFactory.overlapRatio,
                ClusterGAMProcessFactory.overlapRatio.sample);

        FitnessFunctionType functionType = (FitnessFunctionType) Params.getValue(input,
                ClusterGAMProcessFactory.functionType,
                ClusterGAMProcessFactory.functionType.sample);

        Double threshold = (Double) Params.getValue(input, ClusterGAMProcessFactory.threshold,
                ClusterGAMProcessFactory.threshold.sample);

        // start process
        overlapRatio = overlapRatio < 0 ? 0.0 : overlapRatio;
        overlapRatio = overlapRatio > 1 ? 1.0 : overlapRatio;

        SimpleFeatureCollection resultCircles = null;
        GridCoverage2D resultDensity = null;

        ClusterGAMOperation process = new ClusterGAMOperation();

        try {
            process.setOverlapRatio(overlapRatio);
            process.setFunctionType(functionType);
            process.setThreshold(threshold);

            resultCircles = process.execute(popFeatures, popField, caseFeatures, caseField,
                    minRadius, maxRadius, radiusIncrement);
            resultDensity = process.getRaster();
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ClusterGAMProcessFactory.resultCircles.key, resultCircles);
        resultMap.put(ClusterGAMProcessFactory.resultDensity.key, resultDensity);
        return resultMap;
    }
}
