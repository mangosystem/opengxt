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
import org.geotools.process.spatialstatistics.autocorrelation.GlobalMoranIStatisticOperation;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalMoranIStatisticOperation.MoransI;
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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Moran's I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalMoransIProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GlobalMoransIProcess.class);

    private boolean started = false;

    public GlobalMoransIProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static MoransIProcessResult process(SimpleFeatureCollection inputFeatures,
            String inputField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GlobalMoransIProcessFactory.inputFeatures.key, inputFeatures);
        map.put(GlobalMoransIProcessFactory.inputField.key, inputField);
        map.put(GlobalMoransIProcessFactory.spatialConcept.key, spatialConcept);
        map.put(GlobalMoransIProcessFactory.distanceMethod.key, distanceMethod);
        map.put(GlobalMoransIProcessFactory.standardization.key, standardization);
        map.put(GlobalMoransIProcessFactory.searchDistance.key, searchDistance);

        Process process = new GlobalMoransIProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (MoransIProcessResult) resultMap.get(GlobalMoransIProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new MoransIProcessResult(inputFeatures.getSchema().getTypeName(), inputField,
                new MoransI());
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
                    input, GlobalMoransIProcessFactory.inputFeatures, null);
            String inputField = (String) Params.getValue(input,
                    GlobalMoransIProcessFactory.inputField, null);
            if (inputFeatures == null || inputField == null) {
                throw new NullPointerException("inputFeatures and inputField parameters required");
            }

            inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
            if (inputFeatures.getSchema().indexOf(inputField) == -1) {
                throw new NullPointerException(inputField + " field does not exist!");
            }

            SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                    GlobalMoransIProcessFactory.spatialConcept,
                    GlobalMoransIProcessFactory.spatialConcept.sample);

            DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                    GlobalMoransIProcessFactory.distanceMethod,
                    GlobalMoransIProcessFactory.distanceMethod.sample);

            StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                    GlobalMoransIProcessFactory.standardization,
                    GlobalMoransIProcessFactory.standardization.sample);

            Double searchDistance = (Double) Params.getValue(input,
                    GlobalMoransIProcessFactory.searchDistance,
                    GlobalMoransIProcessFactory.searchDistance.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            String typeName = inputFeatures.getSchema().getTypeName();
            MoransIProcessResult processResult = null;
            try {
                GlobalMoranIStatisticOperation process = new GlobalMoranIStatisticOperation();
                process.setSpatialConceptType(spatialConcept);
                process.setDistanceType(distanceMethod);
                process.setStandardizationType(standardization);

                // searchDistance
                if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
                    process.setDistanceBand(searchDistance);
                }

                MoransI ret = process.execute(inputFeatures, inputField);
                processResult = new MoransIProcessResult(typeName, inputField, ret);
            } catch (Exception e) {
                processResult = new MoransIProcessResult(typeName, inputField, new MoransI());
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(GlobalMoransIProcessFactory.RESULT.key, processResult);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    public static class MoransIProcessResult {

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

        public MoransIProcessResult(String typeName, String propertyName, MoransI ret) {
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

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("TypeName: ").append(typeName).append(separator);
            sb.append("PropertyName: ").append(propertyName).append(separator);
            sb.append("Moran Index: ").append(observed_Index).append(separator);
            sb.append("Expected Index: ").append(expected_Index).append(separator);
            sb.append("Variance: ").append(variance).append(separator);
            sb.append("z Score: ").append(z_Score).append(separator);
            sb.append("p Value: ").append(p_Value).append(separator);
            sb.append("Conceptualization: ").append(conceptualization).append(separator);
            sb.append("DistanceMethod: ").append(distanceMethod).append(separator);
            sb.append("RowStandardization: ").append(rowStandardization).append(separator);
            sb.append("DistanceThreshold: ").append(distanceThreshold).append(separator);

            return sb.toString();
        }
    }
}
