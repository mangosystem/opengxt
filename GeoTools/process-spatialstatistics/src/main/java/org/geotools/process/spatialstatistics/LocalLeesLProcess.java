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
import org.geotools.process.spatialstatistics.autocorrelation.LocalLeesLOperation;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculate Local Lee's L values.
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
public class LocalLeesLProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalLeesLProcess.class);

    private boolean started = false;

    public LocalLeesLProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String xField, String yField, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance, Boolean selfNeighbors, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalLeesLProcessFactory.inputFeatures.key, inputFeatures);
        map.put(LocalLeesLProcessFactory.xField.key, xField);
        map.put(LocalLeesLProcessFactory.yField.key, yField);
        map.put(LocalLeesLProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalLeesLProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalLeesLProcessFactory.standardization.key, standardization);
        map.put(LocalLeesLProcessFactory.searchDistance.key, searchDistance);
        map.put(LocalLeesLProcessFactory.selfNeighbors.key, selfNeighbors);

        Process process = new LocalLeesLProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(LocalLeesLProcessFactory.RESULT.key);
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
                    input, LocalLeesLProcessFactory.inputFeatures, null);
            String xField = (String) Params.getValue(input, LocalLeesLProcessFactory.xField, null);
            String yField = (String) Params.getValue(input, LocalLeesLProcessFactory.yField, null);
            if (inputFeatures == null || xField == null || yField == null) {
                throw new NullPointerException("inputFeatures, xField, yField parameters required");
            }

            xField = FeatureTypes.validateProperty(inputFeatures.getSchema(), xField);
            if (inputFeatures.getSchema().indexOf(xField) == -1) {
                throw new NullPointerException(xField + " field does not exist!");
            }

            yField = FeatureTypes.validateProperty(inputFeatures.getSchema(), yField);
            if (inputFeatures.getSchema().indexOf(yField) == -1) {
                throw new NullPointerException(yField + " field does not exist!");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    LocalLeesLProcessFactory.spatialConcept,
                    LocalLeesLProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    LocalLeesLProcessFactory.distanceMethod,
                    LocalLeesLProcessFactory.distanceMethod.sample);

            StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                    LocalLeesLProcessFactory.standardization,
                    LocalLeesLProcessFactory.standardization.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    LocalLeesLProcessFactory.searchDistance,
                    LocalLeesLProcessFactory.searchDistance.sample);

            Boolean selfNeighbors = (Boolean) Params.getValue(input,
                    LocalLeesLProcessFactory.selfNeighbors,
                    LocalLeesLProcessFactory.selfNeighbors.sample);

            // start process
            SimpleFeatureCollection resultFc = null;
            try {
                LocalLeesLOperation process = new LocalLeesLOperation();
                process.setSpatialConceptType(spatialConcept);
                process.setDistanceType(distanceMethod);
                process.setStandardizationType(standardization);
                process.setSelfNeighbors(selfNeighbors);

                // searchDistance
                if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
                    process.setDistanceBand(searchDistance);
                }

                resultFc = process.execute(inputFeatures, xField, yField);
            } catch (Exception ee) {
                monitor.exceptionOccurred(ee);
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(LocalLeesLProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
