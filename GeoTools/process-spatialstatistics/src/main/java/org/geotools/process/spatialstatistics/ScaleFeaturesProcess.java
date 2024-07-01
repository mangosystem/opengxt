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

import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.ScaleFeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

/**
 * Rescale features using x, y scale values.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ScaleFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ScaleFeaturesProcess.class);

    public ScaleFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            double offsetX, double offsetY, Point anchor, ProgressListener monitor) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        return process(inputFeatures, ff.literal(offsetX), ff.literal(offsetY), anchor, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression offsetX, Expression offsetY, Point anchor, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ScaleFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(ScaleFeaturesProcessFactory.scaleX.key, offsetX);
        map.put(ScaleFeaturesProcessFactory.scaleY.key, offsetY);
        map.put(ScaleFeaturesProcessFactory.anchor.key, anchor);

        Process process = new ScaleFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(ScaleFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ScaleFeaturesProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Expression scaleX = (Expression) Params.getValue(input, ScaleFeaturesProcessFactory.scaleX,
                ScaleFeaturesProcessFactory.scaleX.sample);

        Expression scaleY = (Expression) Params.getValue(input, ScaleFeaturesProcessFactory.scaleY,
                ScaleFeaturesProcessFactory.scaleY.sample);

        Point anchor = (Point) Params.getValue(input, ScaleFeaturesProcessFactory.anchor,
                ScaleFeaturesProcessFactory.anchor.sample);

        // start process
        Coordinate coord = anchor == null ? null : anchor.getCoordinate();
        SimpleFeatureCollection resultFc = DataUtilities.simple(new ScaleFeatureCollection(
                inputFeatures, scaleX, scaleY, coord));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ScaleFeaturesProcessFactory.RESULT.key, DataUtilities.simple(resultFc));
        return resultMap;
    }
}
