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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.transformation.SingleSidedBufferFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Buffers a features using a certain distance expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SingleSidedBufferProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SingleSidedBufferProcess.class);

    public SingleSidedBufferProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression distance, Integer quadrantSegments, ProgressListener monitor) {
        return SingleSidedBufferProcess.process(inputFeatures, distance, DistanceUnit.Default,
                quadrantSegments, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression distance, DistanceUnit distanceUnit, Integer quadrantSegments,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SingleSidedBufferProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SingleSidedBufferProcessFactory.distance.key, distance);
        map.put(SingleSidedBufferProcessFactory.distanceUnit.key, distanceUnit);
        map.put(SingleSidedBufferProcessFactory.quadrantSegments.key, quadrantSegments);

        Process process = new SingleSidedBufferProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SingleSidedBufferProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SingleSidedBufferProcessFactory.inputFeatures, null);

        Expression distance = (Expression) Params.getValue(input,
                SingleSidedBufferProcessFactory.distance, null);

        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                SingleSidedBufferProcessFactory.distanceUnit,
                SingleSidedBufferProcessFactory.distanceUnit.sample);

        Integer quadrantSegments = (Integer) Params.getValue(input,
                SingleSidedBufferProcessFactory.quadrantSegments,
                SingleSidedBufferProcessFactory.quadrantSegments.sample);
        if (inputFeatures == null || distance == null) {
            throw new NullPointerException("All parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new SingleSidedBufferFeatureCollection(inputFeatures, distance,
                        distanceUnit, quadrantSegments));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SingleSidedBufferProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
