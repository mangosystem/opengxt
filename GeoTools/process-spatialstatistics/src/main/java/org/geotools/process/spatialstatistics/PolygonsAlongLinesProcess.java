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
import org.geotools.process.spatialstatistics.transformation.PolygonsAlongLinesFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Create equal distance polygons along lines.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolygonsAlongLinesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PolygonsAlongLinesProcess.class);

    public PolygonsAlongLinesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression distance, Expression width, ProgressListener monitor) {
        return PolygonsAlongLinesProcess.process(inputFeatures, distance, width,
                Double.valueOf(0d), monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression distance, Expression width, Double mergeFactor, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PolygonsAlongLinesProcessFactory.lineFeatures.key, inputFeatures);
        map.put(PolygonsAlongLinesProcessFactory.distance.key, distance);
        map.put(PolygonsAlongLinesProcessFactory.width.key, width);
        map.put(PolygonsAlongLinesProcessFactory.mergeFactor.key, mergeFactor);

        Process process = new PolygonsAlongLinesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(PolygonsAlongLinesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                PolygonsAlongLinesProcessFactory.lineFeatures, null);

        Expression distance = (Expression) Params.getValue(input,
                PolygonsAlongLinesProcessFactory.distance, null);

        Expression width = (Expression) Params.getValue(input,
                PolygonsAlongLinesProcessFactory.width, null);

        Double mergeFactor = (Double) Params.getValue(input,
                PolygonsAlongLinesProcessFactory.mergeFactor,
                PolygonsAlongLinesProcessFactory.mergeFactor.sample);
        if (inputFeatures == null || distance == null || width == null) {
            throw new NullPointerException("inputFeatures, distance, width parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new PolygonsAlongLinesFeatureCollection(inputFeatures, distance, width,
                        mergeFactor));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PolygonsAlongLinesProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
