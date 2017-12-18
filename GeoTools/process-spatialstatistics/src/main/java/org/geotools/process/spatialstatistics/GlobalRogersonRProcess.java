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
import org.geotools.process.spatialstatistics.autocorrelation.GlobalRogersonROperation;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalRogersonROperation.RogersonR;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Detect spatial clusters based on feature locations and attribute values using the Global Rogerson's R statistic.
 * 
 * @reference
 * 
 *            Peter A. Rogerson (1999) "The Detection of Clusters Using a Spatial Version of the Chi-Square Goodness-of-Fit Statistic", Geographical
 *            Analysis, 31:130–147<br>
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GlobalRogersonRProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GlobalRogersonRProcess.class);

    public GlobalRogersonRProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static RogersonRProcessResult process(SimpleFeatureCollection inputFeatures,
            String xField, String yField, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance, Double kappa, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GlobalRogersonRProcessFactory.inputFeatures.key, inputFeatures);
        map.put(GlobalRogersonRProcessFactory.xField.key, xField);
        map.put(GlobalRogersonRProcessFactory.yField.key, yField);
        map.put(GlobalRogersonRProcessFactory.spatialConcept.key, spatialConcept);
        map.put(GlobalRogersonRProcessFactory.distanceMethod.key, distanceMethod);
        map.put(GlobalRogersonRProcessFactory.standardization.key, standardization);
        map.put(GlobalRogersonRProcessFactory.searchDistance.key, searchDistance);
        map.put(GlobalRogersonRProcessFactory.kappa.key, kappa);

        Process process = new GlobalRogersonRProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (RogersonRProcessResult) resultMap.get(GlobalRogersonRProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new RogersonRProcessResult(inputFeatures.getSchema().getTypeName(), xField, yField,
                new RogersonR());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                GlobalRogersonRProcessFactory.inputFeatures, null);
        String xField = (String) Params.getValue(input, GlobalRogersonRProcessFactory.xField, null);
        String yField = (String) Params.getValue(input, GlobalRogersonRProcessFactory.yField, null);
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
                GlobalRogersonRProcessFactory.spatialConcept,
                GlobalRogersonRProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                GlobalRogersonRProcessFactory.distanceMethod,
                GlobalRogersonRProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                GlobalRogersonRProcessFactory.standardization,
                GlobalRogersonRProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                GlobalRogersonRProcessFactory.searchDistance,
                GlobalRogersonRProcessFactory.searchDistance.sample);

        Double kappa = (Double) Params.getValue(input, GlobalRogersonRProcessFactory.kappa,
                GlobalRogersonRProcessFactory.kappa.sample);

        // start process
        String typeName = inputFeatures.getSchema().getTypeName();

        GlobalRogersonROperation process = new GlobalRogersonROperation();
        process.setSpatialConceptType(spatialConcept);
        process.setDistanceType(distanceMethod);
        process.setStandardizationType(standardization);
        process.setKappa(kappa); // default = 1.0

        // searchDistance
        if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
            process.setDistanceBand(searchDistance);
        }

        RogersonR ret = process.execute(inputFeatures, xField, yField);
        RogersonRProcessResult processResult = new RogersonRProcessResult(typeName, xField, yField,
                ret);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(GlobalRogersonRProcessFactory.RESULT.key, processResult);
        return resultMap;
    }

    public static class RogersonRProcessResult {

        String typeName;

        String xField;

        String yField;

        String observed_Index;

        String expected_Index;

        String variance;

        String z_Score;

        String p_Value;

        String conceptualization;

        String distanceMethod;

        String rowStandardization;

        String distanceThreshold;

        String kappa;

        public RogersonRProcessResult(String typeName, String xField, String yField, RogersonR ret) {
            this.typeName = typeName;
            this.xField = xField;
            this.yField = yField;

            this.observed_Index = FormatUtils.format(ret.getObservedIndex());
            this.expected_Index = FormatUtils.format(ret.getExpectedIndex());
            this.variance = FormatUtils.format(ret.getZVariance());
            this.z_Score = FormatUtils.format(ret.getZScore());
            this.p_Value = FormatUtils.format(ret.getPValue());
            this.conceptualization = ret.getConceptualization().toString();
            this.distanceMethod = ret.getDistanceMethod().toString();
            this.rowStandardization = ret.getRowStandardization().toString();
            this.distanceThreshold = FormatUtils.format(ret.getDistanceThreshold());
            this.kappa = FormatUtils.format(ret.getKappa());
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getxField() {
            return xField;
        }

        public void setxField(String xField) {
            this.xField = xField;
        }

        public String getyField() {
            return yField;
        }

        public void setyField(String yField) {
            this.yField = yField;
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

        public String getKappa() {
            return kappa;
        }

        public void setKappa(String kappa) {
            this.kappa = kappa;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("Global Rogerson's R").append(separator);
            sb.append("TypeName: ").append(typeName).append(separator);
            sb.append("PropertyName: ").append(xField).append(" + ").append(yField)
                    .append(separator);
            sb.append("Observed Index: ").append(observed_Index).append(separator);
            sb.append("Expected Index: ").append(expected_Index).append(separator);
            sb.append("Variance: ").append(variance).append(separator);
            sb.append("z Score: ").append(z_Score).append(separator);
            sb.append("p Value: ").append(p_Value).append(separator);
            sb.append("Conceptualization: ").append(conceptualization).append(separator);
            sb.append("DistanceMethod: ").append(distanceMethod).append(separator);
            sb.append("RowStandardization: ").append(rowStandardization).append(separator);
            sb.append("DistanceThreshold: ").append(distanceThreshold).append(separator);
            sb.append("Kappa: ").append(kappa).append(separator);

            return sb.toString();
        }
    }
}
