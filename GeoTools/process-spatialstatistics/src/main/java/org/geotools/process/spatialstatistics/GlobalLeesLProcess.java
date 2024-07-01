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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalLeesLOperation;
import org.geotools.process.spatialstatistics.autocorrelation.GlobalLeesLOperation.LeesL;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;

/**
 * Measures spatial autocorrelation based on feature locations and attribute values using the Global Lee's L statistic.
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
public class GlobalLeesLProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GlobalLeesLProcess.class);

    public GlobalLeesLProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static LeesLProcessResult process(SimpleFeatureCollection inputFeatures, String xField,
            String yField, SpatialConcept spatialConcept, DistanceMethod distanceMethod,
            StandardizationMethod standardization, Double searchDistance, Boolean selfNeighbors,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GlobalLeesLProcessFactory.inputFeatures.key, inputFeatures);
        map.put(GlobalLeesLProcessFactory.xField.key, xField);
        map.put(GlobalLeesLProcessFactory.yField.key, yField);
        map.put(GlobalLeesLProcessFactory.spatialConcept.key, spatialConcept);
        map.put(GlobalLeesLProcessFactory.distanceMethod.key, distanceMethod);
        map.put(GlobalLeesLProcessFactory.standardization.key, standardization);
        map.put(GlobalLeesLProcessFactory.searchDistance.key, searchDistance);
        map.put(GlobalLeesLProcessFactory.selfNeighbors.key, selfNeighbors);

        Process process = new GlobalLeesLProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (LeesLProcessResult) resultMap.get(GlobalLeesLProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new LeesLProcessResult(inputFeatures.getSchema().getTypeName(), xField, yField,
                new LeesL());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                GlobalLeesLProcessFactory.inputFeatures, null);
        String xField = (String) Params.getValue(input, GlobalLeesLProcessFactory.xField, null);
        String yField = (String) Params.getValue(input, GlobalLeesLProcessFactory.yField, null);
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
                GlobalLeesLProcessFactory.spatialConcept,
                GlobalLeesLProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                GlobalLeesLProcessFactory.distanceMethod,
                GlobalLeesLProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                GlobalLeesLProcessFactory.standardization,
                GlobalLeesLProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                GlobalLeesLProcessFactory.searchDistance,
                GlobalLeesLProcessFactory.searchDistance.sample);

        Boolean selfNeighbors = (Boolean) Params.getValue(input,
                GlobalLeesLProcessFactory.selfNeighbors,
                GlobalLeesLProcessFactory.selfNeighbors.sample);

        // start process
        String typeName = inputFeatures.getSchema().getTypeName();

        GlobalLeesLOperation process = new GlobalLeesLOperation();
        process.setSpatialConceptType(spatialConcept);
        process.setDistanceType(distanceMethod);
        process.setStandardizationType(standardization);
        process.setSelfNeighbors(selfNeighbors);

        // searchDistance
        if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
            process.setDistanceBand(searchDistance);
        }

        LeesL ret = process.execute(inputFeatures, xField, yField);
        LeesLProcessResult processResult = new LeesLProcessResult(typeName, xField, yField, ret);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(GlobalLeesLProcessFactory.RESULT.key, processResult);
        return resultMap;
    }

    public static class LeesLProcessResult {

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

        public LeesLProcessResult(String typeName, String xField, String yField, LeesL ret) {
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

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("Global Lee's L").append(separator);
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

            return sb.toString();
        }
    }
}
