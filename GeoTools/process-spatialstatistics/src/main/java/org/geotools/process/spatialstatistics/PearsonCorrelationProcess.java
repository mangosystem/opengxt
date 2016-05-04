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
import org.geotools.process.spatialstatistics.operations.PearsonOperation;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Pearson product-moment correlation coefficient (sometimes referred to as the PPMCC or PCC or Pearson's r) is a measure of the linear correlation
 * (dependence) between two variables X and Y, giving a value between +1 and −1 inclusive, where 1 is total positive correlation, 0 is no correlation,
 * and −1 is total negative correlation.
 * 
 * @reference : http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PearsonCorrelationProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PearsonCorrelationProcess.class);

    private boolean started = false;

    public PearsonCorrelationProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static PearsonResult process(SimpleFeatureCollection inputFeatures, String inputFields,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PearsonCorrelationProcessFactory.inputFeatures.key, inputFeatures);
        map.put(PearsonCorrelationProcessFactory.inputFields.key, inputFields);

        Process process = new PearsonCorrelationProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (PearsonResult) resultMap.get(PearsonCorrelationProcessFactory.RESULT.key);
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

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, PearsonCorrelationProcessFactory.inputFeatures, null);
            String inputFields = (String) Params.getValue(input,
                    PearsonCorrelationProcessFactory.inputFields, null);
            if (inputFeatures == null || inputFields == null) {
                throw new NullPointerException("All parameters required");
            }

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            PearsonOperation operation = new PearsonOperation();
            PearsonResult ret = operation.execute(inputFeatures, inputFields);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(PearsonCorrelationProcessFactory.RESULT.key, ret);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
            started = false;
        }
    }
}
