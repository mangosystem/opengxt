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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.OffsetFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Offset features using x, y offset values.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OffsetFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(OffsetFeaturesProcess.class);

    public OffsetFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            double offsetX, double offsetY, ProgressListener monitor) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        return process(inputFeatures, ff.literal(offsetX), ff.literal(offsetY), monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression offsetX, Expression offsetY, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(OffsetFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(OffsetFeaturesProcessFactory.offsetX.key, offsetX);
        map.put(OffsetFeaturesProcessFactory.offsetY.key, offsetY);

        Process process = new OffsetFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(OffsetFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                OffsetFeaturesProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Expression offsetX = (Expression) Params.getValue(input,
                OffsetFeaturesProcessFactory.offsetX, OffsetFeaturesProcessFactory.offsetX.sample);

        Expression offsetY = (Expression) Params.getValue(input,
                OffsetFeaturesProcessFactory.offsetY, OffsetFeaturesProcessFactory.offsetY.sample);

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new OffsetFeatureCollection(
                inputFeatures, offsetX, offsetY));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(OffsetFeaturesProcessFactory.RESULT.key, DataUtilities.simple(resultFc));
        return resultMap;
    }
}
