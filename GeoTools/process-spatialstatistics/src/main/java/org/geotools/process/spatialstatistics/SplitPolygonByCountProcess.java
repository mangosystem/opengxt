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
import org.geotools.process.spatialstatistics.transformation.SplitPolygonFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Splits polygon features based on count or count expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitPolygonByCountProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SplitPolygonByCountProcess.class);

    public SplitPolygonByCountProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection polygonFeatures,
            Expression count, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SplitPolygonByCountProcessFactory.polygonFeatures.key, polygonFeatures);
        map.put(SplitPolygonByCountProcessFactory.count.key, count);

        Process process = new SplitPolygonByCountProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SplitPolygonByCountProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SplitPolygonByCountProcessFactory.polygonFeatures, null);
        Expression count = (Expression) Params.getValue(input,
                SplitPolygonByCountProcessFactory.count);
        if (polygonFeatures == null || count == null) {
            throw new NullPointerException("polygonFeatures, count expression parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new SplitPolygonFeatureCollection(
                polygonFeatures, count));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(AreaProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
