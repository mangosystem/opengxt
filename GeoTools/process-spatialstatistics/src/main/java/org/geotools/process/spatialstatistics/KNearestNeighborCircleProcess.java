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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.pattern.KNearestNeighborCircleOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a k-nearest neighbor circle polygons from two features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KNearestNeighborCircleProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(KNearestNeighborCircleProcess.class);

    public KNearestNeighborCircleProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures, Integer neighbor, Double maximumDistance,
            DistanceUnit distanceUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KNearestNeighborCircleProcessFactory.inputFeatures.key, inputFeatures);
        map.put(KNearestNeighborCircleProcessFactory.nearFeatures.key, nearFeatures);
        map.put(KNearestNeighborCircleProcessFactory.neighbor.key, neighbor);
        map.put(KNearestNeighborCircleProcessFactory.maximumDistance.key, maximumDistance);
        map.put(KNearestNeighborCircleProcessFactory.distanceUnit.key, distanceUnit);

        Process process = new KNearestNeighborCircleProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(KNearestNeighborCircleProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                KNearestNeighborCircleProcessFactory.inputFeatures, null);
        SimpleFeatureCollection nearFeatures = (SimpleFeatureCollection) Params.getValue(input,
                KNearestNeighborCircleProcessFactory.nearFeatures, null);
        if (inputFeatures == null || nearFeatures == null) {
            throw new NullPointerException("inputFeatures, nearFeatures parameters required");
        }

        Integer neighbor = (Integer) Params.getValue(input,
                KNearestNeighborCircleProcessFactory.neighbor,
                KNearestNeighborCircleProcessFactory.neighbor.sample);

        Double maximumDistance = (Double) Params.getValue(input,
                KNearestNeighborCircleProcessFactory.maximumDistance,
                KNearestNeighborCircleProcessFactory.maximumDistance.sample);

        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                KNearestNeighborCircleProcessFactory.distanceUnit,
                KNearestNeighborCircleProcessFactory.distanceUnit.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            KNearestNeighborCircleOperation process = new KNearestNeighborCircleOperation();
            resultFc = process.execute(inputFeatures, nearFeatures, neighbor, maximumDistance,
                    distanceUnit);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(KNearestNeighborCircleProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
