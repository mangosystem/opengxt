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
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Measures the degree of clustering for either high values or low values using the Getis-Ord General G statistic. 
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class GlobalGStatisticsProcess extends AbstractProcess {
    protected static final Logger LOGGER = Logging.getLogger(GlobalGStatisticsProcess.class);

    private boolean started = false;

    public GlobalGStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static String process(SimpleFeatureCollection inputFeatures, String inputField,
            SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GlobalGStatisticsProcessFactory.inputFeatures.key, inputFeatures);
        map.put(GlobalGStatisticsProcessFactory.inputField.key, inputField);
        map.put(GlobalGStatisticsProcessFactory.spatialConcept.key, spatialConcept);
        map.put(GlobalGStatisticsProcessFactory.distanceMethod.key, distanceMethod);
        map.put(GlobalGStatisticsProcessFactory.standardization.key, standardization);
        map.put(GlobalGStatisticsProcessFactory.searchDistance.key, searchDistance);

        Process process = new GlobalGStatisticsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (String) resultMap.get(GlobalGStatisticsProcessFactory.RESULT.key);
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
                    input, GlobalGStatisticsProcessFactory.inputFeatures, null);
            String inputField = (String) Params.getValue(input,
                    GlobalGStatisticsProcessFactory.inputField, null);
            if (inputFeatures == null || inputField == null) {
                throw new NullPointerException("inputFeatures and inputField parameters required");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    GlobalGStatisticsProcessFactory.spatialConcept,
                    GlobalGStatisticsProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    GlobalGStatisticsProcessFactory.distanceMethod,
                    GlobalGStatisticsProcessFactory.distanceMethod.sample);

            StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                    GlobalGStatisticsProcessFactory.standardization,
                    GlobalGStatisticsProcessFactory.standardization.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    GlobalGStatisticsProcessFactory.searchDistance,
                    GlobalGStatisticsProcessFactory.searchDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            // TODO : impelement
            Object result = inputFeatures.toString();
            
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(GlobalGStatisticsProcessFactory.RESULT.key, result);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

}
