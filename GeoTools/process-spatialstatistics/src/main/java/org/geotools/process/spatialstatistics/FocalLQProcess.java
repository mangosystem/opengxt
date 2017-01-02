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
import org.geotools.process.spatialstatistics.autocorrelation.FocalLQOperation;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates a Focal Location Quotients (Focal LQ).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FocalLQProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FocalLQProcess.class);

    private boolean started = false;

    public FocalLQProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String xField, String yField, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FocalLQProcessFactory.inputFeatures.key, inputFeatures);
        map.put(FocalLQProcessFactory.xField.key, xField);
        map.put(FocalLQProcessFactory.yField.key, yField);
        map.put(FocalLQProcessFactory.spatialConcept.key, spatialConcept);
        map.put(FocalLQProcessFactory.distanceMethod.key, distanceMethod);
        map.put(FocalLQProcessFactory.searchDistance.key, searchDistance);

        Process process = new FocalLQProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(FocalLQProcessFactory.RESULT.key);
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
                    input, FocalLQProcessFactory.inputFeatures, null);
            String xField = (String) Params.getValue(input, FocalLQProcessFactory.xField, null);
            String yField = (String) Params.getValue(input, FocalLQProcessFactory.yField, null);
            if (inputFeatures == null || xField == null || yField == null) {
                throw new NullPointerException("inputFeatures, xField, yField parameters required");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    FocalLQProcessFactory.spatialConcept,
                    FocalLQProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    FocalLQProcessFactory.distanceMethod,
                    FocalLQProcessFactory.distanceMethod.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    FocalLQProcessFactory.searchDistance,
                    FocalLQProcessFactory.searchDistance.sample);

            // start process
            FocalLQOperation process = new FocalLQOperation();
            process.setSpatialConceptType(spatialConcept);
            process.setDistanceType(distanceMethod);
            process.setDistanceBand(searchDistance);
            SimpleFeatureCollection resultFc = process.execute(inputFeatures, xField, yField);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(FocalLQProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
