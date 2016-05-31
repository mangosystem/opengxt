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
import org.geotools.process.spatialstatistics.transformation.SpatialClumpFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Creates a spatial clump map using point features and radius expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialClumpMapProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(SpatialClumpMapProcess.class);

    private boolean started = false;

    public SpatialClumpMapProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression radius, Integer quadrantSegments, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SpatialClumpMapProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SpatialClumpMapProcessFactory.radius.key, radius);
        map.put(SpatialClumpMapProcessFactory.quadrantSegments.key, quadrantSegments);

        Process process = new SpatialClumpMapProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SpatialClumpMapProcessFactory.RESULT.key);
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
            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, SpatialClumpMapProcessFactory.inputFeatures, null);
            Expression radius = (Expression) Params.getValue(input,
                    SpatialClumpMapProcessFactory.radius, null);
            Integer quadrantSegments = (Integer) Params.getValue(input,
                    SpatialClumpMapProcessFactory.quadrantSegments,
                    SpatialClumpMapProcessFactory.quadrantSegments.sample);
            if (inputFeatures == null || radius == null) {
                throw new NullPointerException("All parameters required");
            }

            // start process
            SimpleFeatureCollection resultFc = DataUtilities
                    .simple(new SpatialClumpFeatureCollection(inputFeatures, radius,
                            quadrantSegments));
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(SpatialClumpMapProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
