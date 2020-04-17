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
package org.geotools.process.spatialstatistics.relationship;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.CholeskyDecomposition;
import org.ejml.simple.SimpleMatrix;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.SSUtils.StatEnum;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Diagnostics;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variables;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variables.Variable;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.RegressionItem;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.ResidualItem;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.SumItem;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Performs Ordinary Least Squares (OLS) linear regression
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OLSOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(OLSOperation.class);

    private double SST, SSE, SSR, MSR, MSE, STDERR, F, sigF;

    private double meanY, R, R2, R2adjusted, logLik, AIC, AICc;

    private int n, k, dof1, dof2;

    private SimpleMatrix Y; // Dependent

    private SimpleMatrix X; // Independent

    private SimpleMatrix ST; // Variance

    private SimpleMatrix residuals; // Residual

    private int rowCount = 0;

    private Map<String, String> fieldMap;

    private boolean analyzeResidual = true; // Analyze residual or not

    private SimpleFeatureCollection features;

    private SimpleFeatureCollection residualFeatures;

    private OLSResult OLS = new OLSResult();

    public OLSOperation() {

    }

    public OLSResult getOLS() {
        return OLS;
    }

    public void setAnalyzeResidual(boolean analyzeResidual) {
        this.analyzeResidual = analyzeResidual;
    }

    public SimpleFeatureCollection getResidualFeatures() {
        return residualFeatures;
    }

    public OLSResult execute(SimpleFeatureCollection features, String dependentVariable,
            String independentVariables) throws IOException {
        List<String> fields = new ArrayList<String>();

        String[] input_fields = independentVariables.split(",");
        for (String field : input_fields) {
            fields.add(field.trim());
        }

        return execute(features, dependentVariable, fields);
    }

    public OLSResult execute(SimpleFeatureCollection features, String dependentVariable,
            String[] independentVariables) throws IOException {
        return execute(features, dependentVariable, Arrays.asList(independentVariables));
    }

    public OLSResult execute(SimpleFeatureCollection features, String dependentVariable,
            List<String> independentVariables) throws IOException {
        this.features = features;

        // check fields
        fieldMap = checkPropertyName(features.getSchema(), dependentVariable, independentVariables);

        // load dataset
        Map<String, double[]> sampleMap = this.loadSamples(features, fieldMap);

        // create matrix
        int inVarCount = fieldMap.size() - 1; // duplicated fields removed
        Y = new SimpleMatrix(rowCount, 1);
        X = new SimpleMatrix(rowCount, inVarCount + 1);
        X.fill(1.0); // Sets all the elements in this matrix equal to 1

        int column = 0;
        double sumOfY = 0.0d;
        for (Entry<String, double[]> entry : sampleMap.entrySet()) {
            double[] data = entry.getValue();
            if (column == 0) {
                for (int row = 0; row < rowCount; row++) {
                    Y.set(row, column, data[row]);
                    sumOfY += data[row];
                }
            } else {
                X.setColumn(column, 0, data);
            }
            column++;
        }

        // clear
        sampleMap.clear();

        n = X.numRows(); // # of observations
        k = X.numCols() - 1; // # of independent variables
        meanY = sumOfY / n;

        // regression
        return analyze();
    }

    private OLSResult analyze() throws IOException {
        // Step 1: Computes linear regression via Ordinary Least Squares
        // create Z matrix
        SimpleMatrix Z = X.concatColumns(Y);

        SimpleMatrix ZtrZ = Z.transpose().mult(Z);
        SimpleMatrix ss = Y.minus(meanY);

        dof1 = k;
        dof2 = n - k - 1;

        SST = ss.transpose().mult(ss).get(0, 0);
        SSE = Math.pow(ZtrZ.invert().get(k + 1, k + 1), -1);
        SSR = SST - SSE;

        MSR = SSR / dof1;
        MSE = SSE / dof2;

        // Multiple R-Squared
        R2 = SSR / SST;
        R = Math.sqrt(R2);

        // Adjusted R-Squared
        R2adjusted = 1.0 - ((SSE / (n - k - 1)) / (SST / (n - 1)));

        // Standard Error
        STDERR = Math.sqrt(MSE);

        // Probability for JointStat
        // Joint F-Statistic
        F = MSR / MSE;

        // Joint F-Statistic Probability (p-value)
        sigF = Math.abs(SSUtils.fProb(F, dof1, dof2, StatEnum.LEFT));

        // AIC / AICc
        logLik = -(n / 2.0) * (1.0 + Math.log(2.0 * Math.PI)) - (n / 2.0)
                * Math.log(SSE / n);
        double k2 = k + 2;
        AIC = -2.0 * logLik + 2.0 * k2;
        AICc = -2.0 * logLik + 2.0 * k2 * (double) (n / (n - k2 - 1));

        // Step 2: Cholesky Decomposition
        CholeskyDecomposition<DMatrixRMaj> chol = null;
        chol = DecompositionFactory_DDRM.chol(ZtrZ.numRows(), true);
        if (!chol.decompose(ZtrZ.getMatrix().copy())) {
            throw new RuntimeException("Cholesky failed!");
        }

        SimpleMatrix L = SimpleMatrix.wrap(chol.getT(null));
        SimpleMatrix invS = new SimpleMatrix(k + 2, k + 2);
        for (int i = 0; i < k + 2; i++) {
            invS.set(i, i, 1.0 / L.get(i, i));
        }
        SimpleMatrix UnitL = L.mult(invS).invert().extractMatrix(k + 1, k + 2, 0, k + 1);

        // Coefficient
        SimpleMatrix beta = UnitL.transpose().scale(-1.0);
        SimpleMatrix xxi = X.transpose().mult(X).invert().scale(MSE);

        // Columns = {Coefficient, StdError, t-Statistic, Probability}
        ST = new SimpleMatrix(beta.numRows(), 4);
        for (int i = 0; i < k + 1; i++) {
            double coefficient = beta.get(i, 0); // Coefficient
            double stdError = Math.sqrt(xxi.get(i, i)); // StdError
            double tStatistics = coefficient / stdError; // t-Statistic
            double pValue = SSUtils.tProb(dof2, Math.abs(tStatistics), StatEnum.BOTH); // Probability

            ST.set(i, 0, coefficient);
            ST.set(i, 1, stdError);
            ST.set(i, 2, tStatistics);
            ST.set(i, 3, pValue);
        }

        // build OLS Summary
        this.buildOLSResult();
        if (false == analyzeResidual) {
            return OLS;
        }

        // Step 3: Residuals
        // Columns = {Estimated, Residual, StdResid, StdResid2}
        residuals = new SimpleMatrix(rowCount, 4);

        double sumOfVals = 0.0d;
        double sumOfSqrs = 0.0d;

        final SimpleMatrix multXBeta = X.mult(beta);
        for (int i = 0; i < n; i++) {
            double residual = Y.get(i, 0) - multXBeta.get(i, 0);
            double estimated = Y.get(i, 0) - residual;

            sumOfVals += residual;
            sumOfSqrs += residual * residual;

            residuals.set(i, 0, estimated); // Estimated
            residuals.set(i, 1, residual); // Residual
        }

        // 표준잔차 = 잔차 / 잔차의 표준편차
        double meanRes = sumOfVals / n;
        double stdDev = Math.sqrt((sumOfSqrs - Math.pow(sumOfVals, 2.0) / n) / (n - 1));
        for (int i = 0; i < n; i++) {
            double residual = residuals.get(i, 1);
            double stdRes = (residual - meanRes) / STDERR;
            double stdRes2 = residual / stdDev;

            residuals.set(i, 2, stdRes); // StdResid = 내적 스튜던트 잔차
            residuals.set(i, 3, stdRes2); // StdResid2 = 표준잔차
        }

        // finally build features
        buildFeatures();

        return OLS;
    }

    private void buildOLSResult() {
        // Diagnostics
        Diagnostics diagnostics = OLS.getDiagnostics();
        diagnostics.setR(R);
        diagnostics.setRSquared(R2);
        diagnostics.setAdjustedRSquared(R2adjusted);
        diagnostics.setStandardError(STDERR);
        diagnostics.setNumberOfObservations(n);
        diagnostics.setAIC(AIC);
        diagnostics.setAICc(AICc);

        // Variation
        Variance variance = OLS.getVariance();
        RegressionItem regress = variance.getRegression();
        regress.setDegreesOfFreedom(dof1);
        regress.setSumOfSquare(SSR);
        regress.setSquareMean(MSR);
        regress.setfStatistic(F);
        regress.setfProbability(sigF);

        ResidualItem residual = variance.getResidual();
        residual.setDegreesOfFreedom(dof2);
        residual.setSumOfSquare(SSE);
        residual.setSquareMean(MSE);

        SumItem sum = variance.getSum();
        sum.setDegreesOfFreedom(n - 1);
        sum.setSumOfSquare(SST);

        // Variables
        Variables variables = OLS.getVariables();

        Object[] fields = fieldMap.keySet().toArray();
        for (int row = 0; row < ST.numRows(); row++) {
            String name = row == 0 ? "Intercept" : fields[row].toString();

            Variable variable = new Variable(name);
            variable.setAttributes(ST.get(row, 0), ST.get(row, 1), ST.get(row, 2), ST.get(row, 3));
            variables.getItems().add(variable);
        }
    }

    private boolean buildFeatures() throws IOException {
        // create schema
        String[] fields = { "Estimated", "Residual", "StdResid", "StdResid2" };

        SimpleFeatureType featureType = features.getSchema();
        for (int index = 0; index < fields.length; index++) {
            featureType = FeatureTypes.add(featureType, fields[index], Double.class, 19);
        }

        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // write features
        SimpleFeatureIterator featureIter = features.features();
        try {
            int row = 0;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                newFeature.setAttribute(fields[0], residuals.get(row, 0));
                newFeature.setAttribute(fields[1], residuals.get(row, 1));
                newFeature.setAttribute(fields[2], residuals.get(row, 2));
                newFeature.setAttribute(fields[3], residuals.get(row, 3));

                featureWriter.write(newFeature);
                row++;

            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        this.residualFeatures = featureWriter.getFeatureCollection();
        return this.residualFeatures != null;
    }

    private Map<String, double[]> loadSamples(SimpleFeatureCollection features,
            Map<String, String> fieldMap) {
        rowCount = features.size();
        Map<String, double[]> sampleMap = new LinkedHashMap<String, double[]>();
        for (String key : fieldMap.keySet()) {
            sampleMap.put(key, new double[rowCount]);
        }

        SimpleFeatureIterator featureIter = features.features();
        try {
            int index = 0;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                for (String key : fieldMap.keySet()) {
                    Double valueX = Converters.convert(feature.getAttribute(key), Double.class);
                    if (valueX == null) {
                        sampleMap.get(key)[index] = 0.0d;
                        LOGGER.log(Level.WARNING, feature.getID() + "'s " + key
                                + " value is null. Replaced null to zero!");
                    } else {
                        sampleMap.get(key)[index] = valueX.doubleValue();
                    }
                }
                index++;
            }
        } finally {
            featureIter.close();
        }

        return sampleMap;
    }

    private Map<String, String> checkPropertyName(SimpleFeatureType schema,
            String dependentVariable, List<String> independentVariables) {
        Map<String, String> fieldMap = new LinkedHashMap<String, String>();
        fieldMap.put(dependentVariable, dependentVariable);

        for (String field : independentVariables) {
            field = FeatureTypes.validateProperty(schema, field.trim());
            if (fieldMap.containsKey(field)) {
                LOGGER.log(Level.WARNING, field + " property already exist!");
            } else {
                fieldMap.put(field, field);
            }
        }

        return fieldMap;
    }

    public void printResult() {
        DecimalFormat format = new DecimalFormat("0.0000000000");

        System.out.println("\n----------------------------------------------------------------");
        System.out.println("#. OLS Diagnostics");
        System.out.println("Number of Observations = " + n);
        System.out.println("R = " + format.format(R));
        System.out.println("R-Squared = " + format.format(R2));
        System.out.println("Adjusted R-Squared = " + format.format(R2adjusted));
        System.out.println("Standard Error = " + format.format(STDERR));
        System.out.println("Log Likelihood = " + format.format(logLik));
        System.out.println("Akaike's Information Criterion (AIC) = " + format.format(AIC));
        System.out.println("Corrected Akaike's Information Criterion (AICc) = " + format.format(AICc));

        System.out.println("\n#. OLS Results - Model Variables");
        System.out
                .println("\t\tDoF\tSum of squares\tSquare mean\tJoint F-Statistic\tJoint F-Statistic Probability");
        System.out.println("Regression\t" + dof1 + "\t" + format.format(SSR) + "\t"
                + format.format(MSR) + "\t" + format.format(F) + "\t" + format.format(sigF));
        System.out.println("Residual\t" + dof2 + "\t" + format.format(SSE) + "\t "
                + format.format(MSE));
        System.out.println("Sum\t\t " + (n - 1) + "\t" + format.format(SST));

        System.out.println("\nVariable\tCoefficient\tStdError\tt-Statistic\tProbability");

        Object[] fields = fieldMap.keySet().toArray();
        for (int row = 0; row < ST.numRows(); row++) {
            String title = row == 0 ? "Y Intercept" : fields[row].toString() + "\t";
            String coef = format.format(ST.get(row, 0));
            String stErr = format.format(ST.get(row, 1));
            String tStat = format.format(ST.get(row, 2));
            String pVal = format.format(ST.get(row, 3));

            System.out.println(title + "\t" + coef + "\t" + stErr + "\t " + tStat + "\t" + pVal);
        }
        System.out.println("----------------------------------------------------------------\n");
    }
}
