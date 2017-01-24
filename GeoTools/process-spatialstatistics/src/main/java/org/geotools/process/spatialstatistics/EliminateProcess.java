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
import org.geotools.process.spatialstatistics.operations.EliminateOperation;
import org.geotools.process.spatialstatistics.operations.EliminateOperation.EliminateOption;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Eliminates sliver polygons by merging them with neighboring polygons that have the largest or smallest area or the longest shared border.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class EliminateProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(EliminateProcess.class);

    public EliminateProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            EliminateOption option, Filter exception, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(EliminateProcessFactory.inputFeatures.key, inputFeatures);
        map.put(EliminateProcessFactory.option.key, option);
        map.put(EliminateProcessFactory.exception.key, exception);

        Process process = new EliminateProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(EliminateProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return inputFeatures;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                EliminateProcessFactory.inputFeatures, null);
        EliminateOption option = (EliminateOption) Params.getValue(input,
                EliminateProcessFactory.option, EliminateProcessFactory.option.sample);
        Filter exception = (Filter) Params.getValue(input, EliminateProcessFactory.exception,
                EliminateProcessFactory.exception.sample);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            EliminateOperation operation = new EliminateOperation();
            resultFc = operation.execute(inputFeatures, option, exception);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(EliminateProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
