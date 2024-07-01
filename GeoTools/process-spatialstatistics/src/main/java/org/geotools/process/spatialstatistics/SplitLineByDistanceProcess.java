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
import org.geotools.process.spatialstatistics.transformation.SplitByDistanceFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Splits line features based on distance or distance expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitLineByDistanceProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SplitLineByDistanceProcess.class);

    public SplitLineByDistanceProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            Expression distance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SplitLineByDistanceProcessFactory.lineFeatures.key, lineFeatures);
        map.put(SplitLineByDistanceProcessFactory.distance.key, distance);

        Process process = new SplitLineByDistanceProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SplitLineByDistanceProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SplitLineByDistanceProcessFactory.lineFeatures, null);
        Expression distance = (Expression) Params.getValue(input,
                SplitLineByDistanceProcessFactory.distance);
        if (lineFeatures == null || distance == null) {
            throw new NullPointerException("lineFeatures, distance expression parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new SplitByDistanceFeatureCollection(lineFeatures, distance));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(AreaProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
