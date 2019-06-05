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
import org.geotools.process.spatialstatistics.operations.SimplifyPolygonFeaturesOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Simplifies polygon outlines by removing relatively extraneous vertices while preserving shape.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SimplifyPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SimplifyPolygonProcess.class);

    public SimplifyPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Double tolerance, Boolean preserveTopology, ProgressListener monitor) {
        return process(inputFeatures, tolerance, preserveTopology, 0d, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Double tolerance, Boolean preserveTopology, Double minimumArea, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SimplifyPolygonProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SimplifyPolygonProcessFactory.tolerance.key, tolerance);
        map.put(SimplifyPolygonProcessFactory.preserveTopology.key, preserveTopology);
        map.put(SimplifyPolygonProcessFactory.minimumArea.key, minimumArea);

        Process process = new SimplifyPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(SimplifyPolygonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SimplifyPolygonProcessFactory.inputFeatures, null);
        Double tolerance = (Double) Params.getValue(input, SimplifyPolygonProcessFactory.tolerance,
                SimplifyPolygonProcessFactory.tolerance.sample);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Boolean preserveTopology = (Boolean) Params.getValue(input,
                SimplifyPolygonProcessFactory.preserveTopology,
                SimplifyPolygonProcessFactory.preserveTopology.sample);

        Double minimumArea = (Double) Params.getValue(input,
                SimplifyPolygonProcessFactory.minimumArea,
                SimplifyPolygonProcessFactory.minimumArea.sample);

        // start process
        SimpleFeatureCollection resultFc = inputFeatures;
        if (tolerance > 0d) {
            SimplifyPolygonFeaturesOperation process = new SimplifyPolygonFeaturesOperation();
            try {
                resultFc = process.execute(inputFeatures, tolerance, preserveTopology, minimumArea);
            } catch (IOException e) {
                throw new ProcessException(e);
            }
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SimplifyPolygonProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
