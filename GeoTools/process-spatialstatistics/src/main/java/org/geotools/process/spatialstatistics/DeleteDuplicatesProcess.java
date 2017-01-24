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
import org.geotools.process.spatialstatistics.operations.DeleteDuplicateFeaturesOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Finds duplicated geometries(not attributes) in features and removes them.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DeleteDuplicatesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(DeleteDuplicatesProcess.class);

    public DeleteDuplicatesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DeleteDuplicatesProcessFactory.inputFeatures.key, inputFeatures);

        Process process = new DeleteDuplicatesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(DeleteDuplicatesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                DeleteDuplicatesProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            DeleteDuplicateFeaturesOperation operation = new DeleteDuplicateFeaturesOperation();
            resultFc = operation.execute(inputFeatures);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(DeleteDuplicatesProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
