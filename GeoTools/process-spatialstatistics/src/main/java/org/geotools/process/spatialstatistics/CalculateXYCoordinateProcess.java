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

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.XYCalculationFeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Adds the fields x and y to the input features and calculates their coordinate values.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateXYCoordinateProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CalculateXYCoordinateProcess.class);

    public CalculateXYCoordinateProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String xField, String yField, Boolean inside, CoordinateReferenceSystem targetCRS,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CalculateXYCoordinateProcessFactory.inputFeatures.key, inputFeatures);
        map.put(CalculateXYCoordinateProcessFactory.xField.key, xField);
        map.put(CalculateXYCoordinateProcessFactory.yField.key, yField);
        map.put(CalculateXYCoordinateProcessFactory.inside.key, inside);
        map.put(CalculateXYCoordinateProcessFactory.targetCRS.key, targetCRS);

        Process process = new CalculateXYCoordinateProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(CalculateXYCoordinateProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                CalculateXYCoordinateProcessFactory.inputFeatures, null);

        String xField = (String) Params.getValue(input, CalculateXYCoordinateProcessFactory.xField,
                CalculateXYCoordinateProcessFactory.xField.sample);

        String yField = (String) Params.getValue(input, CalculateXYCoordinateProcessFactory.yField,
                CalculateXYCoordinateProcessFactory.yField.sample);
        if (inputFeatures == null || xField == null || xField.trim().length() == 0
                || yField == null || yField.trim().length() == 0) {
            throw new NullPointerException("inputFeatures, xField, yField parameters required");
        }

        Boolean inside = (Boolean) Params.getValue(input,
                CalculateXYCoordinateProcessFactory.inside,
                CalculateXYCoordinateProcessFactory.inside.sample);

        CoordinateReferenceSystem targetCRS = (CoordinateReferenceSystem) Params.getValue(input,
                CalculateXYCoordinateProcessFactory.targetCRS, null);

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new XYCalculationFeatureCollection(
                inputFeatures, xField, yField, inside, targetCRS));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(CalculateXYCoordinateProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
