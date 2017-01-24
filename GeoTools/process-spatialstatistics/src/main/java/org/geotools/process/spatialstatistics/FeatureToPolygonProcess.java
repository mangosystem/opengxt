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
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.FeatureToPolygonOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a feature class containing polygons generated from areas enclosed by input line or polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureToPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FeatureToPolygonProcess.class);

    public FeatureToPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Double tolerance, SimpleFeatureCollection labelFeatures, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FeatureToPolygonProcessFactory.inputFeatures.key, inputFeatures);
        map.put(FeatureToPolygonProcessFactory.tolerance.key, tolerance);
        map.put(FeatureToPolygonProcessFactory.labelFeatures.key, labelFeatures);

        Process process = new FeatureToPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(FeatureToPolygonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                FeatureToPolygonProcessFactory.inputFeatures, null);

        Double tolerance = (Double) Params.getValue(input,
                FeatureToPolygonProcessFactory.tolerance,
                FeatureToPolygonProcessFactory.tolerance.sample);

        SimpleFeatureCollection labelFeatures = (SimpleFeatureCollection) Params.getValue(input,
                FeatureToPolygonProcessFactory.labelFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            FeatureToPolygonOperation operation = new FeatureToPolygonOperation();
            resultFc = operation.execute(inputFeatures, tolerance, labelFeatures);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(FeatureToPolygonProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
