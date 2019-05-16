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
import org.geotools.process.spatialstatistics.transformation.RotateFeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Rotate features using anchor point and angle in degree unit.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RotateFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RotateFeaturesProcess.class);

    public RotateFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Point anchor, double angle, ProgressListener monitor) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        return process(inputFeatures, anchor, ff.literal(angle), monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Point anchor, Expression angle, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RotateFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(RotateFeaturesProcessFactory.anchor.key, anchor);
        map.put(RotateFeaturesProcessFactory.angle.key, angle);

        Process process = new RotateFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(RotateFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                RotateFeaturesProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Point anchor = (Point) Params.getValue(input, RotateFeaturesProcessFactory.anchor,
                RotateFeaturesProcessFactory.anchor.sample);

        Expression angle = (Expression) Params.getValue(input, RotateFeaturesProcessFactory.angle,
                RotateFeaturesProcessFactory.angle.sample);

        // start process
        Coordinate coord = anchor == null ? null : anchor.getCoordinate();
        SimpleFeatureCollection resultFc = DataUtilities.simple(new RotateFeatureCollection(
                inputFeatures, coord, angle));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RotateFeaturesProcessFactory.RESULT.key, DataUtilities.simple(resultFc));
        return resultMap;
    }
}
