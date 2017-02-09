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
import org.geotools.process.spatialstatistics.transformation.WedgeBufferFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Creates wedge shaped buffers on point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WedgeBufferProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(WedgeBufferProcess.class);

    public WedgeBufferProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection pointFeatures,
            Expression azimuth, Expression wedgeAngle, Expression innerRadius,
            Expression outerRadius, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(WedgeBufferProcessFactory.pointFeatures.key, pointFeatures);
        map.put(WedgeBufferProcessFactory.azimuth.key, azimuth);
        map.put(WedgeBufferProcessFactory.wedgeAngle.key, wedgeAngle);
        map.put(WedgeBufferProcessFactory.innerRadius.key, innerRadius);
        map.put(WedgeBufferProcessFactory.outerRadius.key, outerRadius);

        Process process = new WedgeBufferProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(WedgeBufferProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(input,
                WedgeBufferProcessFactory.pointFeatures, null);
        Expression azimuth = (Expression) Params.getValue(input, WedgeBufferProcessFactory.azimuth,
                null);
        Expression wedgeAngle = (Expression) Params.getValue(input,
                WedgeBufferProcessFactory.wedgeAngle, null);
        Expression innerRadius = (Expression) Params.getValue(input,
                WedgeBufferProcessFactory.innerRadius, null);
        Expression outerRadius = (Expression) Params.getValue(input,
                WedgeBufferProcessFactory.outerRadius, null);

        if (pointFeatures == null || azimuth == null || wedgeAngle == null || outerRadius == null) {
            throw new NullPointerException(
                    "pointFeatures, azimuth, wedgeAngle, outerRadius parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new WedgeBufferFeatureCollection(
                pointFeatures, azimuth, wedgeAngle, innerRadius, outerRadius));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(WedgeBufferProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
