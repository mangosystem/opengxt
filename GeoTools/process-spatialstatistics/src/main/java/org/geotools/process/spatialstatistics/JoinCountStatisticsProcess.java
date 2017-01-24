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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.autocorrelation.JoinCountStatisticsOperation;
import org.geotools.process.spatialstatistics.autocorrelation.JoinCountStatisticsOperation.JoinCount;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.ContiguityType;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Measure global spatial autocorrelation for binary data, i.e., with observations coded as 1 or B (for Black) and 0 or W (for White).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class JoinCountStatisticsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(JoinCountStatisticsProcess.class);

    public JoinCountStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static JoinCountProcessResult process(SimpleFeatureCollection inputFeatures,
            Filter blackExpression, ContiguityType contiguityType, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(JoinCountStatisticsProcessFactory.inputFeatures.key, inputFeatures);
        map.put(JoinCountStatisticsProcessFactory.blackExpression.key, blackExpression);
        map.put(JoinCountStatisticsProcessFactory.contiguityType.key, contiguityType);

        Process process = new JoinCountStatisticsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (JoinCountProcessResult) resultMap
                    .get(JoinCountStatisticsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new JoinCountProcessResult(new JoinCount(inputFeatures.getSchema().getTypeName(),
                contiguityType));
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                JoinCountStatisticsProcessFactory.inputFeatures, null);

        Filter blackExpression = (Filter) Params.getValue(input,
                JoinCountStatisticsProcessFactory.blackExpression, null);
        if (inputFeatures == null || blackExpression == null) {
            throw new NullPointerException("inputFeatures, blackExpression parameters required");
        }

        ContiguityType contiguityType = (ContiguityType) Params.getValue(input,
                JoinCountStatisticsProcessFactory.contiguityType,
                JoinCountStatisticsProcessFactory.contiguityType.sample);

        // start process
        String typeName = inputFeatures.getSchema().getTypeName();
        JoinCount joinCount = new JoinCount(typeName, contiguityType);
        try {
            JoinCountStatisticsOperation operation = new JoinCountStatisticsOperation();
            joinCount = operation.execute(inputFeatures, blackExpression, contiguityType);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(JoinCountStatisticsProcessFactory.RESULT.key, new JoinCountProcessResult(
                joinCount));
        return resultMap;
    }

    public static class JoinCountProcessResult {

        String typeName;

        ContiguityType contiguityType = ContiguityType.Queen;

        int featureCount = 0;

        int blackCount = 0;

        int whiteCount = 0;

        int numberOfJoins = 0;

        int observedBB = 0;

        int observedWW = 0;

        int observedBW = 0;

        double expectedBB = 0;

        double expectedWW = 0;

        double expectedBW = 0;

        double stdDevBB = 0;

        double stdDevWW = 0;

        double stdDevBW = 0;

        double zScoreBB = 0;

        double zScoreWW = 0;

        double zScoreBW = 0;

        public JoinCountProcessResult(JoinCount joinCount) {
            this.typeName = joinCount.getTypeName();
            this.contiguityType = joinCount.getContiguityType();
            this.featureCount = joinCount.getFeatureCount();
            this.blackCount = joinCount.getBlackCount();
            this.whiteCount = joinCount.getWhiteCount();
            this.numberOfJoins = joinCount.getNumberOfJoins();
            this.observedBB = joinCount.getObservedBB();
            this.observedBW = joinCount.getObservedBW();
            this.observedWW = joinCount.getObservedWW();
            this.expectedBB = joinCount.getExpectedBB();
            this.expectedBW = joinCount.getExpectedBW();
            this.expectedWW = joinCount.getExpectedWW();
            this.stdDevBB = joinCount.getStdDevBB();
            this.stdDevBW = joinCount.getStdDevBW();
            this.stdDevWW = joinCount.getStdDevWW();
            this.zScoreBB = joinCount.getzScoreBB();
            this.zScoreBW = joinCount.getzScoreBW();
            this.zScoreWW = joinCount.getzScoreWW();
        }

        public String getTypeName() {
            return typeName;
        }

        public ContiguityType getContiguityType() {
            return contiguityType;
        }

        public int getFeatureCount() {
            return featureCount;
        }

        public int getBlackCount() {
            return blackCount;
        }

        public int getWhiteCount() {
            return whiteCount;
        }

        public int getNumberOfJoins() {
            return numberOfJoins;
        }

        public int getObservedBB() {
            return observedBB;
        }

        public int getObservedWW() {
            return observedWW;
        }

        public int getObservedBW() {
            return observedBW;
        }

        public double getExpectedBB() {
            return expectedBB;
        }

        public double getExpectedWW() {
            return expectedWW;
        }

        public double getExpectedBW() {
            return expectedBW;
        }

        public double getStdDevBB() {
            return stdDevBB;
        }

        public double getStdDevWW() {
            return stdDevWW;
        }

        public double getStdDevBW() {
            return stdDevBW;
        }

        public double getzScoreBB() {
            return zScoreBB;
        }

        public double getzScoreWW() {
            return zScoreWW;
        }

        public double getzScoreBW() {
            return zScoreBW;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString() {
            final String sep = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("Type Name: ").append(getTypeName()).append(sep);
            sb.append("Contiguity Type: ").append(getContiguityType().toString()).append(sep);

            sb.append("Number of Features: ").append(getFeatureCount()).append(sep);
            sb.append("Number of Black: ").append(getBlackCount()).append(sep);
            sb.append("Number of White: ").append(getWhiteCount()).append(sep);
            sb.append("Number of Joins: ").append(getNumberOfJoins()).append(sep);

            sb.append("Observed BB Joins: ").append(getObservedBB()).append(sep);
            sb.append("Observed WW Joins: ").append(getObservedWW()).append(sep);
            sb.append("Observed BW Joins: ").append(getObservedBW()).append(sep);

            sb.append("Expected BB Joins: ").append(getExpectedBB()).append(sep);
            sb.append("Expected WW Joins: ").append(getExpectedWW()).append(sep);
            sb.append("Expected BW Joins: ").append(getExpectedBW()).append(sep);

            sb.append("Std Dev of Expected BB Joins: ").append(getStdDevBB()).append(sep);
            sb.append("Std Dev of Expected WW Joins: ").append(getStdDevWW()).append(sep);
            sb.append("Std Dev of Expected BW Joins: ").append(getStdDevBW()).append(sep);

            sb.append("Z-statistics BB Joins: ").append(getzScoreBB()).append(sep);
            sb.append("Z-statistics WW Joins: ").append(getzScoreWW()).append(sep);
            sb.append("Z-statistics BW Joins: ").append(getzScoreBW()).append(sep);

            return sb.toString();
        }
    }
}
