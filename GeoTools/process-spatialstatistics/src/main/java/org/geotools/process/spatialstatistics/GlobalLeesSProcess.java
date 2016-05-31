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
import org.geotools.process.spatialstatistics.autocorrelation.GlobalLeesSOperation;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalLeesSOperation.LeesS;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Lee's S statistic.
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
public class GlobalLeesSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GlobalLeesSProcess.class);

    private boolean started = false;

    public GlobalLeesSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static LeesSProcessResult process(SimpleFeatureCollection inputFeatures,
            String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GlobalLeesSProcessFactory.inputFeatures.key, inputFeatures);
        map.put(GlobalLeesSProcessFactory.inputField.key, inputField);
        map.put(GlobalLeesSProcessFactory.spatialConcept.key, spatialConcept);
        map.put(GlobalLeesSProcessFactory.distanceMethod.key, distanceMethod);
        map.put(GlobalLeesSProcessFactory.standardization.key, standardization);
        map.put(GlobalLeesSProcessFactory.searchDistance.key, searchDistance);

        Process process = new GlobalLeesSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (LeesSProcessResult) resultMap.get(GlobalLeesSProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new LeesSProcessResult(inputFeatures.getSchema().getTypeName(), inputField,
                new LeesS());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, GlobalLeesSProcessFactory.inputFeatures, null);
            String inputField = (String) Params.getValue(input,
                    GlobalLeesSProcessFactory.inputField, null);
            if (inputFeatures == null || inputField == null) {
                throw new NullPointerException("inputFeatures and inputField parameters required");
            }

            inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
            if (inputFeatures.getSchema().indexOf(inputField) == -1) {
                throw new NullPointerException(inputField + " field does not exist!");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    GlobalLeesSProcessFactory.spatialConcept,
                    GlobalLeesSProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    GlobalLeesSProcessFactory.distanceMethod,
                    GlobalLeesSProcessFactory.distanceMethod.sample);

            StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                    GlobalLeesSProcessFactory.standardization,
                    GlobalLeesSProcessFactory.standardization.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    GlobalLeesSProcessFactory.searchDistance,
                    GlobalLeesSProcessFactory.searchDistance.sample);

            // start process
            String typeName = inputFeatures.getSchema().getTypeName();
            LeesSProcessResult processResult = null;
            try {
                GlobalLeesSOperation process = new GlobalLeesSOperation();
                process.setSpatialConceptType(spatialConcept);
                process.setDistanceType(distanceMethod);
                process.setStandardizationType(standardization);

                // searchDistance
                if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
                    process.setDistanceBand(searchDistance);
                }

                LeesS ret = process.execute(inputFeatures, inputField);
                processResult = new LeesSProcessResult(typeName, inputField, ret);
            } catch (Exception e) {
                processResult = new LeesSProcessResult(typeName, inputField, new LeesS());
            }
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(GlobalLeesSProcessFactory.RESULT.key, processResult);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

    public static class LeesSProcessResult {

        String typeName;

        String propertyName;

        String observed_Index;

        String expected_Index;

        String variance;

        String z_Score;

        String p_Value;

        String conceptualization;

        String distanceMethod;

        String rowStandardization;

        String distanceThreshold;

        public LeesSProcessResult(String typeName, String propertyName, LeesS ret) {
            this.typeName = typeName;
            this.propertyName = propertyName;

            this.observed_Index = FormatUtils.format(ret.getObservedIndex());
            this.expected_Index = FormatUtils.format(ret.getExpectedIndex());
            this.variance = FormatUtils.format(ret.getZVariance());
            this.z_Score = FormatUtils.format(ret.getZScore());
            this.p_Value = FormatUtils.format(ret.getPValue());
            this.conceptualization = ret.getConceptualization().toString();
            this.distanceMethod = ret.getDistanceMethod().toString();
            this.rowStandardization = ret.getRowStandardization().toString();
            this.distanceThreshold = FormatUtils.format(ret.getDistanceThreshold());
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getObserved_Index() {
            return observed_Index;
        }

        public void setObserved_Index(String observed_Index) {
            this.observed_Index = observed_Index;
        }

        public String getExpected_Index() {
            return expected_Index;
        }

        public void setExpected_Index(String expected_Index) {
            this.expected_Index = expected_Index;
        }

        public String getVariance() {
            return variance;
        }

        public void setVariance(String variance) {
            this.variance = variance;
        }

        public String getZ_Score() {
            return z_Score;
        }

        public void setZ_Score(String z_Score) {
            this.z_Score = z_Score;
        }

        public String getP_Value() {
            return p_Value;
        }

        public void setP_Value(String p_Value) {
            this.p_Value = p_Value;
        }

        public String getConceptualization() {
            return conceptualization;
        }

        public void setConceptualization(String conceptualization) {
            this.conceptualization = conceptualization;
        }

        public String getDistanceMethod() {
            return distanceMethod;
        }

        public void setDistanceMethod(String distanceMethod) {
            this.distanceMethod = distanceMethod;
        }

        public String getRowStandardization() {
            return rowStandardization;
        }

        public void setRowStandardization(String rowStandardization) {
            this.rowStandardization = rowStandardization;
        }

        public String getDistanceThreshold() {
            return distanceThreshold;
        }

        public void setDistanceThreshold(String distanceThreshold) {
            this.distanceThreshold = distanceThreshold;
        }
    }
}
