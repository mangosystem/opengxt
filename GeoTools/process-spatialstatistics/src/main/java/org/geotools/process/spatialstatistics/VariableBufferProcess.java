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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.transformation.VariableBufferFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Creates a buffer polygon with a varying buffer distance at each vertex along a line.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class VariableBufferProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(VariableBufferProcess.class);

    public VariableBufferProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            Expression startDistance, Expression endDistance, DistanceUnit distanceUnit,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(VariableBufferProcessFactory.lineFeatures.key, lineFeatures);
        map.put(VariableBufferProcessFactory.startDistance.key, startDistance);
        map.put(VariableBufferProcessFactory.endDistance.key, endDistance);
        map.put(VariableBufferProcessFactory.distanceUnit.key, distanceUnit);

        Process process = new VariableBufferProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(VariableBufferProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                VariableBufferProcessFactory.lineFeatures, null);

        Expression startDistance = (Expression) Params.getValue(input,
                VariableBufferProcessFactory.startDistance,
                VariableBufferProcessFactory.startDistance.sample);

        Expression endDistance = (Expression) Params.getValue(input,
                VariableBufferProcessFactory.endDistance,
                VariableBufferProcessFactory.endDistance.sample);

        if (lineFeatures == null || startDistance == null || endDistance == null) {
            throw new NullPointerException(
                    "lineFeatures, startDistance, endDistance parameters required");
        }

        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                VariableBufferProcessFactory.distanceUnit,
                VariableBufferProcessFactory.distanceUnit.sample);

        // start process
        SimpleFeatureCollection resultFc = new VariableBufferFeatureCollection(lineFeatures,
                startDistance, endDistance, distanceUnit);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(VariableBufferProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
