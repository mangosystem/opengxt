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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.SnapPointsToLinesOperation;
import org.geotools.util.logging.Logging;

/**
 * Snaps each point in the point features to the closest point on the nearest line in the line features, provided it is within the user defined snap
 * tolerance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SnapPointsToLinesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SnapPointsToLinesProcess.class);

    public SnapPointsToLinesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection pointFeatures,
            SimpleFeatureCollection lineFeatures, double tolerance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SnapPointsToLinesProcessFactory.pointFeatures.key, pointFeatures);
        map.put(SnapPointsToLinesProcessFactory.lineFeatures.key, lineFeatures);
        map.put(SnapPointsToLinesProcessFactory.tolerance.key, tolerance);

        Process process = new SnapPointsToLinesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SnapPointsToLinesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SnapPointsToLinesProcessFactory.pointFeatures, null);

        SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SnapPointsToLinesProcessFactory.lineFeatures, null);
        if (lineFeatures == null || pointFeatures == null) {
            throw new NullPointerException("pointFeatures, lineFeatures parameters required");
        }

        Double tolerance = (Double) Params.getValue(input,
                SnapPointsToLinesProcessFactory.tolerance,
                SnapPointsToLinesProcessFactory.tolerance.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            SnapPointsToLinesOperation operation = new SnapPointsToLinesOperation();
            resultFc = operation.execute(pointFeatures, lineFeatures, tolerance);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SnapPointsToLinesProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
