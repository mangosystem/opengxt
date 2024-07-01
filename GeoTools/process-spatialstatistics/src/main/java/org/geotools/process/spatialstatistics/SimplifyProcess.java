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

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.SimplifyFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Simplifies the input features using the Douglas-Peucker simplification algorithm with a specified maximum offset tolerance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SimplifyProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SimplifyProcess.class);

    public SimplifyProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression tolerance, Boolean preserveTopology, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SimplifyProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SimplifyProcessFactory.tolerance.key, tolerance);
        map.put(SimplifyProcessFactory.preserveTopology.key, preserveTopology);

        Process process = new SimplifyProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(SimplifyProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SimplifyProcessFactory.inputFeatures, null);
        Expression tolerance = (Expression) Params.getValue(input,
                SimplifyProcessFactory.tolerance, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Boolean preserveTopology = (Boolean) Params.getValue(input,
                SimplifyProcessFactory.preserveTopology,
                SimplifyProcessFactory.preserveTopology.sample);

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new SimplifyFeatureCollection(
                inputFeatures, tolerance, preserveTopology));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SimplifyProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
