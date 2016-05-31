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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.PointsAlongLinesFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Create points along lines.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsAlongLinesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PointsAlongLinesProcess.class);

    private boolean started = false;

    public PointsAlongLinesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            Expression distance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PointsAlongLinesProcessFactory.lineFeatures.key, lineFeatures);
        map.put(PointsAlongLinesProcessFactory.distance.key, distance);

        Process process = new PointsAlongLinesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(PointsAlongLinesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                    PointsAlongLinesProcessFactory.lineFeatures, null);
            Expression distance = (Expression) Params.getValue(input,
                    PointsAlongLinesProcessFactory.distance);
            if (lineFeatures == null || distance == null) {
                throw new NullPointerException(
                        "lineFeatures, distance expression parameters required");
            }

            // start process
            SimpleFeatureCollection resultFc = DataUtilities
                    .simple(new PointsAlongLinesFeatureCollection(lineFeatures, distance));
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AreaProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
