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
import org.geotools.process.spatialstatistics.operations.TrimLineOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Removes portions of a line that extend a specified distance past a line intersection (dangles).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TrimLineProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(TrimLineProcess.class);

    private boolean started = false;

    public TrimLineProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection lineFeatures,
            Double dangleLength, Boolean deleteShort, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TrimLineProcessFactory.lineFeatures.key, lineFeatures);
        map.put(TrimLineProcessFactory.dangleLength.key, dangleLength);
        map.put(TrimLineProcessFactory.deleteShort.key, deleteShort);

        Process process = new TrimLineProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(TrimLineProcessFactory.RESULT.key);
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
                    input, TrimLineProcessFactory.lineFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameter required");
            }

            Double dangleLength = (Double) Params
                    .getValue(input, TrimLineProcessFactory.dangleLength,
                            TrimLineProcessFactory.dangleLength.sample);
            if (dangleLength == null || dangleLength <= 0) {
                throw new NullPointerException("dangleLength parameter must be greater than 0");
            }

            Boolean deleteShort = (Boolean) Params.getValue(input,
                    TrimLineProcessFactory.deleteShort, TrimLineProcessFactory.deleteShort.sample);

            // start process
            TrimLineOperation operation = new TrimLineOperation();
            SimpleFeatureCollection resultFc = operation.execute(inputFeatures, dangleLength,
                    deleteShort);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(TrimLineProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
