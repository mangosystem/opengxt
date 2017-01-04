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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.KMeansClusterOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * k-means clustering is a method of cluster analysis which aims to partition n observations into k clusters in which each observation belongs to the
 * cluster with the nearest mean
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KMeansClusteringProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(KMeansClusteringProcess.class);

    private boolean started = false;

    public KMeansClusteringProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String targetField, Integer numberOfClusters, Boolean asCircle, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KMeansClusteringProcessFactory.inputFeatures.key, inputFeatures);
        map.put(KMeansClusteringProcessFactory.targetField.key, targetField);
        map.put(KMeansClusteringProcessFactory.numberOfClusters.key, numberOfClusters);
        map.put(KMeansClusteringProcessFactory.asCircle.key, asCircle);

        Process process = new KMeansClusteringProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(KMeansClusteringProcessFactory.RESULT.key);
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
            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, KMeansClusteringProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameters required");
            }

            String targetField = (String) Params.getValue(input,
                    KMeansClusteringProcessFactory.targetField,
                    KMeansClusteringProcessFactory.targetField.sample);
            if (targetField == null) {
                throw new NullPointerException("targetField parameters required");
            }

            int numberOfClusters = (Integer) Params.getValue(input,
                    KMeansClusteringProcessFactory.numberOfClusters,
                    KMeansClusteringProcessFactory.numberOfClusters.sample);
            if (numberOfClusters < 1) {
                throw new NullPointerException("Number of clusters must be greater than 1");
            }

            Boolean asCircle = (Boolean) Params.getValue(input,
                    KMeansClusteringProcessFactory.asCircle,
                    KMeansClusteringProcessFactory.asCircle.sample);

            // start process
            KMeansClusterOperation operator = new KMeansClusterOperation();
            SimpleFeatureCollection resultFc = null;
            if (asCircle) {
                resultFc = operator.executeAsCircle(inputFeatures, targetField, numberOfClusters);
            } else {
                resultFc = operator.execute(inputFeatures, targetField, numberOfClusters);
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AreaProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
