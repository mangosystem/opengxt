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
import org.geotools.process.spatialstatistics.operations.SplitLineAtPointOperation;
import org.geotools.util.logging.Logging;

/**
 * Splits line features based on intersection or proximity to point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitLineAtPointProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SplitLineAtPointProcess.class);

    public SplitLineAtPointProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            SimpleFeatureCollection pointFeatures, double tolerance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SplitLineAtPointProcessFactory.lineFeatures.key, lineFeatures);
        map.put(SplitLineAtPointProcessFactory.pointFeatures.key, pointFeatures);
        map.put(SplitLineAtPointProcessFactory.tolerance.key, tolerance);

        Process process = new SplitLineAtPointProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SplitLineAtPointProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SplitLineAtPointProcessFactory.lineFeatures, null);

        SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SplitLineAtPointProcessFactory.pointFeatures, null);
        if (lineFeatures == null || pointFeatures == null) {
            throw new NullPointerException("pointFeatures, lineFeatures parameters required");
        }

        Double tolerance = (Double) Params.getValue(input,
                SplitLineAtPointProcessFactory.tolerance,
                SplitLineAtPointProcessFactory.tolerance.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            SplitLineAtPointOperation operation = new SplitLineAtPointOperation();
            resultFc = operation.execute(lineFeatures, pointFeatures, tolerance);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SplitLineAtPointProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
