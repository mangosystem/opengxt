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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.QuadratOperation;
import org.geotools.process.spatialstatistics.pattern.QuadratOperation.QuadratResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Performs a point pattern analysis using quadrat method.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class QuadratAnalysisProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(QuadratAnalysisProcess.class);

    public QuadratAnalysisProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static QuadratResult process(SimpleFeatureCollection inputFeatures, Double cellSize,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(QuadratAnalysisProcessFactory.inputFeatures.key, inputFeatures);
        map.put(QuadratAnalysisProcessFactory.cellSize.key, cellSize);

        Process process = new QuadratAnalysisProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (QuadratResult) resultMap.get(QuadratAnalysisProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                QuadratAnalysisProcessFactory.inputFeatures, null);
        Double cellSize = (Double) Params.getValue(input, QuadratAnalysisProcessFactory.cellSize,
                QuadratAnalysisProcessFactory.cellSize.sample);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        QuadratOperation operator = new QuadratOperation();
        QuadratResult result = operator.execute(inputFeatures, cellSize);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(QuadratAnalysisProcessFactory.RESULT.key, result);
        return resultMap;
    }

}
