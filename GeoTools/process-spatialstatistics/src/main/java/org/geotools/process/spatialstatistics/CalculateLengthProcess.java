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
import org.geotools.process.spatialstatistics.transformation.LengthCalculationFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Adds the length field to the input features and calculates feature's length.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateLengthProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CalculateLengthProcess.class);

    public CalculateLengthProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String lengthField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CalculateLengthProcessFactory.inputFeatures.key, inputFeatures);
        map.put(CalculateLengthProcessFactory.lengthField.key, lengthField);

        Process process = new CalculateLengthProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(CalculateLengthProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                CalculateLengthProcessFactory.inputFeatures, null);
        String lengthField = (String) Params.getValue(input,
                CalculateLengthProcessFactory.lengthField,
                CalculateLengthProcessFactory.lengthField.sample);
        if (inputFeatures == null || lengthField == null || lengthField.trim().length() == 0) {
            throw new NullPointerException("inputFeatures, lengthField parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new LengthCalculationFeatureCollection(inputFeatures, lengthField));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(CalculateLengthProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
