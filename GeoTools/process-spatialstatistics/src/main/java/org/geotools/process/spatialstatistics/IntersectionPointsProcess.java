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
import org.geotools.process.spatialstatistics.operations.IntersectionPointsOperation;
import org.geotools.util.logging.Logging;

/**
 * Creates point features where the lines in the input features intersect the lines in the intersect features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IntersectionPointsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(IntersectionPointsProcess.class);

    public IntersectionPointsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection intersectFeatures, String intersectIDField,

            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(IntersectionPointsProcessFactory.inputFeatures.key, inputFeatures);
        map.put(IntersectionPointsProcessFactory.intersectFeatures.key, intersectFeatures);
        map.put(IntersectionPointsProcessFactory.intersectIDField.key, intersectIDField);

        Process process = new IntersectionPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(IntersectionPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                IntersectionPointsProcessFactory.inputFeatures, null);
        SimpleFeatureCollection intersectFeatures = (SimpleFeatureCollection) Params.getValue(
                input, IntersectionPointsProcessFactory.intersectFeatures, null);
        if (inputFeatures == null || intersectFeatures == null) {
            throw new NullPointerException("inputFeatures, intersectFeatures parameters required");
        }

        String intersectIDField = (String) Params.getValue(input,
                IntersectionPointsProcessFactory.intersectIDField, null);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            IntersectionPointsOperation operation = new IntersectionPointsOperation();
            resultFc = operation.execute(inputFeatures, intersectFeatures, intersectIDField);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(IntersectionPointsProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
