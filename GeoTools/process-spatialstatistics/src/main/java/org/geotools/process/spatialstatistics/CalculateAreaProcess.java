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
import org.geotools.process.spatialstatistics.transformation.AreaCalculationFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates feature's area or perimeter.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateAreaProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CalculateAreaProcess.class);

    private boolean started = false;

    public CalculateAreaProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String areaField, String perimeterField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CalculateAreaProcessFactory.inputFeatures.key, inputFeatures);
        map.put(CalculateAreaProcessFactory.areaField.key, areaField);
        map.put(CalculateAreaProcessFactory.perimeterField.key, perimeterField);

        Process process = new CalculateAreaProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(CalculateAreaProcessFactory.RESULT.key);
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
                    input, CalculateAreaProcessFactory.inputFeatures, null);

            String areaField = (String) Params.getValue(input,
                    CalculateAreaProcessFactory.areaField,
                    CalculateAreaProcessFactory.areaField.sample);

            String perimeterField = (String) Params.getValue(input,
                    CalculateAreaProcessFactory.perimeterField, null);
            if (inputFeatures == null || areaField == null || areaField.trim().length() == 0) {
                throw new NullPointerException("inputFeatures, areaField parameters required");
            }

            // start process
            if (perimeterField != null && perimeterField.trim().length() == 0) {
                perimeterField = null;
            }
            SimpleFeatureCollection resultFc = new AreaCalculationFeatureCollection(inputFeatures,
                    areaField, perimeterField);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(CalculateAreaProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
