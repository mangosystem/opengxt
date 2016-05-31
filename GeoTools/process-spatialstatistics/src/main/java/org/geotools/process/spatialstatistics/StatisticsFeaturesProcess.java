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
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates summary statistics(Sum, Minimum, Maximum, Mean, Standard Deviation etc.) for field(s) in a featurecollection
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(StatisticsFeaturesProcess.class);

    private boolean started = false;

    public StatisticsFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static DataStatisticsResult process(SimpleFeatureCollection inputFeatures,
            String inputFields, ProgressListener monitor) {
        return StatisticsFeaturesProcess.process(inputFeatures, inputFields, null, monitor);
    }

    public static DataStatisticsResult process(SimpleFeatureCollection inputFeatures,
            String inputFields, String caseField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(StatisticsFeaturesProcessFactory.inputFeatures.key, inputFeatures);
        map.put(StatisticsFeaturesProcessFactory.inputFields.key, inputFields);
        map.put(StatisticsFeaturesProcessFactory.caseField.key, caseField);

        Process process = new StatisticsFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (DataStatisticsResult) resultMap
                    .get(StatisticsFeaturesProcessFactory.RESULT.key);
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
                    input, StatisticsFeaturesProcessFactory.inputFeatures, null);
            String inputFields = (String) Params.getValue(input,
                    StatisticsFeaturesProcessFactory.inputFields, null);
            if (inputFeatures == null || inputFields == null) {
                throw new NullPointerException("inputFeatures, inputFields parameters required");
            }

            String caseField = (String) Params.getValue(input,
                    StatisticsFeaturesProcessFactory.caseField, null);

            // start process
            DataStatisticsOperation operator = new DataStatisticsOperation();
            DataStatisticsResult result = operator.execute(inputFeatures, inputFields, caseField);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(StatisticsFeaturesProcessFactory.RESULT.key, result);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
