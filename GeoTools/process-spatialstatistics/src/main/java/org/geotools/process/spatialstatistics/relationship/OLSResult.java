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

import java.util.ArrayList;
import java.util.List;

/**
 * Performs Ordinary Least Squares (OLS) linear regression result for WPS PPIO
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OLSResult {

    Diagnostics diagnostics = new Diagnostics();

    Variance variance = new Variance();

    Variables variables = new Variables();

    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public Variance getVariance() {
        return variance;
    }

    public void setVariation(Variance variance) {
        this.variance = variance;
    }

    public Variables getVariables() {
        return variables;
    }

    public void setVariables(Variables variables) {
        this.variables = variables;
    }

    public static class Diagnostics {

        Double R = Double.valueOf(0.0);

        Double RSquared = Double.valueOf(0.0);

        Double adjustedRSquared = Double.valueOf(0.0);

        Double standardError = Double.valueOf(0.0);

        Integer numberOfObservations = Integer.valueOf(0);

        public Double getR() {
            return R;
        }

        public void setR(Double r) {
            R = r;
        }

        public Double getRSquared() {
            return RSquared;
        }

        public void setRSquared(Double rSquared) {
            RSquared = rSquared;
        }

        public Double getAdjustedRSquared() {
            return adjustedRSquared;
        }

        public void setAdjustedRSquared(Double adjustedRSquared) {
            this.adjustedRSquared = adjustedRSquared;
        }

        public Double getStandardError() {
            return standardError;
        }

        public void setStandardError(Double standardError) {
            this.standardError = standardError;
        }

        public Integer getNumberOfObservations() {
            return numberOfObservations;
        }

        public void setNumberOfObservations(Integer numberOfObservations) {
            this.numberOfObservations = numberOfObservations;
        }

    }

    public static class Variance {

        RegressionItem regression = new RegressionItem();

        ResidualItem residual = new ResidualItem();

        SumItem sum = new SumItem();

        public RegressionItem getRegression() {
            return regression;
        }

        public void setRegression(RegressionItem regression) {
            this.regression = regression;
        }

        public ResidualItem getResidual() {
            return residual;
        }

        public void setResidual(ResidualItem residual) {
            this.residual = residual;
        }

        public SumItem getSum() {
            return sum;
        }

        public void setSum(SumItem sum) {
            this.sum = sum;
        }

        public static class RegressionItem {

            Integer degreesOfFreedom = Integer.valueOf(0);

            Double sumOfSquare = Double.valueOf(0.0);

            Double squareMean = Double.valueOf(0.0);

            Double fStatistic = Double.valueOf(0.0);

            Double fProbability = Double.valueOf(0.0);

            public Integer getDegreesOfFreedom() {
                return degreesOfFreedom;
            }

            public void setDegreesOfFreedom(Integer degreesOfFreedom) {
                this.degreesOfFreedom = degreesOfFreedom;
            }

            public Double getSumOfSquare() {
                return sumOfSquare;
            }

            public void setSumOfSquare(Double sumOfSquare) {
                this.sumOfSquare = sumOfSquare;
            }

            public Double getSquareMean() {
                return squareMean;
            }

            public void setSquareMean(Double squareMean) {
                this.squareMean = squareMean;
            }

            public Double getfStatistic() {
                return fStatistic;
            }

            public void setfStatistic(Double fStatistic) {
                this.fStatistic = fStatistic;
            }

            public Double getfProbability() {
                return fProbability;
            }

            public void setfProbability(Double fProbability) {
                this.fProbability = fProbability;
            }

        }

        public static class ResidualItem {

            Integer degreesOfFreedom = Integer.valueOf(0);

            Double sumOfSquare = Double.valueOf(0.0);

            Double squareMean = Double.valueOf(0.0);

            public Integer getDegreesOfFreedom() {
                return degreesOfFreedom;
            }

            public void setDegreesOfFreedom(Integer degreesOfFreedom) {
                this.degreesOfFreedom = degreesOfFreedom;
            }

            public Double getSumOfSquare() {
                return sumOfSquare;
            }

            public void setSumOfSquare(Double sumOfSquare) {
                this.sumOfSquare = sumOfSquare;
            }

            public Double getSquareMean() {
                return squareMean;
            }

            public void setSquareMean(Double squareMean) {
                this.squareMean = squareMean;
            }

        }

        public static class SumItem {

            Integer degreesOfFreedom = Integer.valueOf(0);

            Double sumOfSquare = Double.valueOf(0.0);

            public Integer getDegreesOfFreedom() {
                return degreesOfFreedom;
            }

            public void setDegreesOfFreedom(Integer degreesOfFreedom) {
                this.degreesOfFreedom = degreesOfFreedom;
            }

            public Double getSumOfSquare() {
                return sumOfSquare;
            }

            public void setSumOfSquare(Double sumOfSquare) {
                this.sumOfSquare = sumOfSquare;
            }
        }
    }

    public static class Variables {

        List<Variable> items = new ArrayList<Variable>();

        public List<Variable> getItems() {
            return items;
        }

        public void setItems(List<Variable> items) {
            this.items = items;
        }

        public static class Variable {
            String variable;

            Double coefficient = Double.valueOf(0.0);

            Double stdError = Double.valueOf(0.0);

            Double tStatistic = Double.valueOf(0.0);

            Double probability = Double.valueOf(0.0);

            public Variable(String variable) {
                this.variable = variable;
            }

            public void setAttributes(Double coefficient, Double stdError, Double tStatistic,
                    Double probability) {
                this.coefficient = coefficient;
                this.stdError = stdError;
                this.tStatistic = tStatistic;
                this.probability = probability;
            }

            public String getVariable() {
                return variable;
            }

            public void setVariable(String variable) {
                this.variable = variable;
            }

            public Double getCoefficient() {
                return coefficient;
            }

            public void setCoefficient(Double coefficient) {
                this.coefficient = coefficient;
            }

            public Double getStdError() {
                return stdError;
            }

            public void setStdError(Double stdError) {
                this.stdError = stdError;
            }

            public Double gettStatistic() {
                return tStatistic;
            }

            public void settStatistic(Double tStatistic) {
                this.tStatistic = tStatistic;
            }

            public Double getProbability() {
                return probability;
            }

            public void setProbability(Double probability) {
                this.probability = probability;
            }
        }
    }
}
