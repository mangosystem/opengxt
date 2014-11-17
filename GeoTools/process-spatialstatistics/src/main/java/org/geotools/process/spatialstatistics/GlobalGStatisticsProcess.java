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
import org.geotools.process.spatialstatistics.autocorrelation.GlobalGStatisticOperation;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalGStatisticOperation.GeneralG;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
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

    public static GStatisticsProcessResult process(SimpleFeatureCollection inputFeatures,
            String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
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
            return (GStatisticsProcessResult) resultMap
                    .get(GlobalGStatisticsProcessFactory.RESULT.key);
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

            inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
            if (inputFeatures.getSchema().indexOf(inputField) == -1) {
                throw new NullPointerException(inputField + " field does not exist!");
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
            String typeName = inputFeatures.getSchema().getTypeName();
            GStatisticsProcessResult processResult = null;
            try {
                GlobalGStatisticOperation process = new GlobalGStatisticOperation();
                process.setSpatialConceptType(spatialConcept);
                process.setDistanceType(distanceMethod);
                process.setStandardizationType(standardization);

                // searchDistance
                if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
                    process.setDistanceBand(searchDistance);
                }

                GeneralG ret = process.execute(inputFeatures, inputField);

                processResult = new GStatisticsProcessResult(typeName, inputField,
                        ret.getObservedIndex(), ret.getExpectedIndex(), ret.getZVariance(),
                        ret.getZScore(), ret.getPValue());

                processResult.setConceptualization(ret.getConceptualization().toString());
                processResult.setDistanceMethod(ret.getDistanceMethod().toString());
                processResult.setDistanceThreshold(String.valueOf(ret.getDistanceThreshold()));
                processResult.setRowStandardization(ret.getRowStandardization().toString());

            } catch (Exception e) {
                processResult = new GStatisticsProcessResult(typeName, inputField, 0, 0, 0, 0, 0);
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(GlobalGStatisticsProcessFactory.RESULT.key, processResult);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    public static class GStatisticsProcessResult {

        String typeName;

        String propertyName;

        String Observed_General_G;

        String Expected_General_G;

        String Variance;

        String Z_Score;

        String p_Value;

        String conceptualization;

        String distanceMethod;

        String rowStandardization;

        String distanceThreshold;

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

        public String getObserved_General_G() {
            return Observed_General_G;
        }

        public void setObserved_General_G(String observed_General_G) {
            Observed_General_G = observed_General_G;
        }

        public String getExpected_General_G() {
            return Expected_General_G;
        }

        public void setExpected_General_G(String expected_General_G) {
            Expected_General_G = expected_General_G;
        }

        public String getVariance() {
            return Variance;
        }

        public void setVariance(String variance) {
            Variance = variance;
        }

        public String getZ_Score() {
            return Z_Score;
        }

        public void setZ_Score(String z_Score) {
            Z_Score = z_Score;
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

        public GStatisticsProcessResult(String typeName, String propertyName, double obsGeneralG,
                double expGeneralG, double variance, double z_score, double pValue) {
            this.typeName = typeName;
            this.propertyName = propertyName;

            this.Observed_General_G = FormatUtils.format(obsGeneralG);
            this.Expected_General_G = FormatUtils.format(expGeneralG);
            this.Variance = FormatUtils.format(variance);
            this.Z_Score = FormatUtils.format(z_score);
            this.p_Value = FormatUtils.format(pValue);
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("TypeName: ").append(typeName).append(separator);
            sb.append("PropertyName: ").append(propertyName).append(separator);
            sb.append("Observed_General_G: ").append(Observed_General_G).append(separator);
            sb.append("Expected_General_G: ").append(Expected_General_G).append(separator);
            sb.append("Variance: ").append(Variance).append(separator);
            sb.append("Z_Score: ").append(Z_Score).append(separator);
            sb.append("P_Value: ").append(p_Value).append(separator);
            sb.append("Conceptualization: ").append(conceptualization).append(separator);
            sb.append("DistanceMethod: ").append(distanceMethod).append(separator);
            sb.append("RowStandardization: ").append(rowStandardization).append(separator);
            sb.append("DistanceThreshold: ").append(distanceThreshold).append(separator);

            return sb.toString();
        }
    }

}
