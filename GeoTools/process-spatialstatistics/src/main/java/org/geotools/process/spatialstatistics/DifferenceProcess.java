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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.DifferenceFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Creates a features by overlaying the Input Features with the polygons of the difference features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DifferenceProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(DifferenceProcess.class);

    public DifferenceProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection differenceFeatures, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DifferenceProcessFactory.inputFeatures.key, inputFeatures);
        map.put(DifferenceProcessFactory.differenceFeatures.key, differenceFeatures);

        Process process = new DifferenceProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(DifferenceProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                DifferenceProcessFactory.inputFeatures, null);

        SimpleFeatureCollection differenceFeatures = (SimpleFeatureCollection) Params
                .getValue(input, DifferenceProcessFactory.differenceFeatures, null);
        if (inputFeatures == null || differenceFeatures == null) {
            throw new NullPointerException("inputFeatures, differenceFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = inputFeatures;
        if (inputFeatures.size() > 0 && differenceFeatures.size() > 0) {
            resultFc = new DifferenceFeatureCollection(inputFeatures, differenceFeatures);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(DifferenceProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
