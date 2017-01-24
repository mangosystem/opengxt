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
import org.geotools.process.spatialstatistics.transformation.FlowMapFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Creates a flow map features using an origin-destination line features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FlowMapProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FlowMapProcess.class);

    public FlowMapProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            Expression odValue, Expression doValue, Double maxSize, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FlowMapProcessFactory.lineFeatures.key, lineFeatures);
        map.put(FlowMapProcessFactory.odValue.key, odValue);
        map.put(FlowMapProcessFactory.doValue.key, doValue);
        map.put(FlowMapProcessFactory.maxSize.key, maxSize);

        Process process = new FlowMapProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(FlowMapProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection lineFeatures = (SimpleFeatureCollection) Params.getValue(input,
                FlowMapProcessFactory.lineFeatures, null);
        Expression odValue = (Expression) Params.getValue(input, FlowMapProcessFactory.odValue,
                null);
        Expression doValue = (Expression) Params.getValue(input, FlowMapProcessFactory.doValue,
                null);
        Double maxSize = (Double) Params.getValue(input, FlowMapProcessFactory.maxSize, null);
        if (lineFeatures == null || odValue == null) {
            throw new NullPointerException(
                    "inputFeatures, odValue, inputFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new FlowMapFeatureCollection(
                lineFeatures, odValue, doValue, maxSize));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(FlowMapProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
