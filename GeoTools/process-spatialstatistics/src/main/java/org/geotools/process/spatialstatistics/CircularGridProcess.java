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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.CircularType;
import org.geotools.process.spatialstatistics.operations.CircularGridOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates circular grids from extent or bounds source features
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircularGridProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CircularGridProcess.class);

    public CircularGridProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Double radius, CircularType circularType,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CircularGridProcessFactory.extent.key, extent);
        map.put(CircularGridProcessFactory.boundsSource.key, boundsSource);
        map.put(CircularGridProcessFactory.radius.key, radius);
        map.put(CircularGridProcessFactory.circularType.key, circularType);

        Process process = new CircularGridProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(CircularGridProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        ReferencedEnvelope gridBounds = (ReferencedEnvelope) Params.getValue(input,
                CircularGridProcessFactory.extent, null);
        SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                CircularGridProcessFactory.boundsSource, null);
        if (gridBounds == null) {
            throw new NullPointerException("extent parameters required");
        }

        Double radius = (Double) Params.getValue(input, CircularGridProcessFactory.radius, null);
        CircularType circularType = (CircularType) Params.getValue(input,
                CircularGridProcessFactory.circularType,
                CircularGridProcessFactory.circularType.sample);
        if (radius == null || radius == 0) {
            throw new NullPointerException("sideLen parameter should be grater than 0");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            CircularGridOperation operation = new CircularGridOperation();
            operation.setBoundsSource(boundsSource);
            operation.setCircularType(circularType);
            resultFc = operation.execute(gridBounds, radius);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(CircularGridProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
