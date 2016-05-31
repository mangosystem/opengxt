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
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.pattern.NNIOperation;
import org.geotools.process.spatialstatistics.pattern.NNIOperation.NearestNeighborResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates a nearest neighbor index based on the average distance from each feature to its nearest neighboring feature.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NearestNeighborProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(NearestNeighborProcess.class);

    private boolean started = false;

    public NearestNeighborProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static NearestNeighborResult process(SimpleFeatureCollection inputFeatures,
            DistanceMethod distanceMethod, Double area, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NearestNeighborProcessFactory.inputFeatures.key, inputFeatures);
        map.put(NearestNeighborProcessFactory.distanceMethod.key, distanceMethod);
        map.put(NearestNeighborProcessFactory.area.key, area);

        Process process = new NearestNeighborProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (NearestNeighborResult) resultMap.get(NearestNeighborProcessFactory.RESULT.key);
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
                    input, NearestNeighborProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameters required");
            }
            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    NearestNeighborProcessFactory.distanceMethod,
                    NearestNeighborProcessFactory.distanceMethod.sample);
            Double area = (Double) Params.getValue(input, NearestNeighborProcessFactory.area,
                    Double.valueOf(0.0));

            // start process
            NNIOperation operation = new NNIOperation();
            operation.setDistanceType(distanceMethod);
            if (area == null) {
                area = Double.valueOf(0.0d);
            }
            NearestNeighborResult nni = operation.execute(inputFeatures, area);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(NearestNeighborProcessFactory.RESULT.key, nni);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
