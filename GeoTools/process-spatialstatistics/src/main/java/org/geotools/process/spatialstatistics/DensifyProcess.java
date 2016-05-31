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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.DensifyFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Densifies a geometry by inserting extra vertices along the line segments contained in the geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DensifyProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(DensifyProcess.class);

    private boolean started = false;

    public DensifyProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression tolerance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DensifyProcessFactory.inputFeatures.key, inputFeatures);
        map.put(DensifyProcessFactory.tolerance.key, tolerance);

        Process process = new DensifyProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(DensifyProcessFactory.RESULT.key);
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
                    input, DensifyProcessFactory.inputFeatures, null);
            Expression tolerance = (Expression) Params.getValue(input,
                    DensifyProcessFactory.tolerance, null);
            if (inputFeatures == null || tolerance == null) {
                throw new NullPointerException("inputFeatures, tolerance parameters required");
            }

            // start process
            SimpleFeatureCollection resultFc = new DensifyFeatureCollection(inputFeatures,
                    tolerance);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(DensifyProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
