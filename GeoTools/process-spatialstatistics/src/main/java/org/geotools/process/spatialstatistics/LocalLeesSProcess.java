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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculate Local Lee's S values.
 * 
 * @reference
 * 
 *            Sang-Il Lee (2001) "Developing a bivariate spatial association measure: an integration of Pearson's r and Moran's I", Journal of
 *            Geograhical Systems, 3:369-385<br>
 *            Sang-Il Lee (2004) "A generalized significance testing method for global measures of spatial association: an extension of the Mantel
 *            test", Environment and Planning A 36:1687-1703<br>
 *            Sang-Il Lee (2009) "A generalized randomization approach to local measures of spatial association", Geographical Analysis 41:221-248<br>
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalLeesSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalLeesSProcess.class);

    private boolean started = false;

    public LocalLeesSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalLeesSProcessFactory.inputFeatures.key, inputFeatures);
        map.put(LocalLeesSProcessFactory.inputField.key, inputField);
        map.put(LocalLeesSProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalLeesSProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalLeesSProcessFactory.standardization.key, standardization);
        map.put(LocalLeesSProcessFactory.searchDistance.key, searchDistance);

        Process process = new LocalLeesSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(LocalLeesSProcessFactory.RESULT.key);
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
                    input, LocalLeesSProcessFactory.inputFeatures, null);
            String inputField = (String) Params.getValue(input,
                    LocalLeesSProcessFactory.inputField, null);
            if (inputFeatures == null || inputField == null) {
                throw new NullPointerException("inputFeatures and inputField parameters required");
            }

            inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
            if (inputFeatures.getSchema().indexOf(inputField) == -1) {
                throw new NullPointerException(inputField + " field does not exist!");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    LocalLeesSProcessFactory.spatialConcept,
                    LocalLeesSProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    LocalLeesSProcessFactory.distanceMethod,
                    LocalLeesSProcessFactory.distanceMethod.sample);

            StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                    LocalLeesSProcessFactory.standardization,
                    LocalLeesSProcessFactory.standardization.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    LocalLeesSProcessFactory.searchDistance,
                    LocalLeesSProcessFactory.searchDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            // TODO code here
            SimpleFeatureCollection resultFc = inputFeatures;
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(LocalLeesSProcessFactory.RESULT.key, resultFc);
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
