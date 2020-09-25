/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.process.spatialstatistics.pattern;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.pattern.AbstractClusterOperation.FitnessFunctionType;
import org.geotools.util.logging.Logging;

/**
 * Fitness Function
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @see https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public class FitnessFunction {
    protected static final Logger LOGGER = Logging.getLogger(FitnessFunction.class);

    static final int MAX_CASES = 300;

    // Significance threshold
    private double threshold = 0.01; // default

    private int minExpected = 1; // minimum population to be considered

    private int minCases = 1; // minimum number of cases to be considered

    private FitnessFunctionType functionType = FitnessFunctionType.Poisson;

    private final double[] cons = new double[MAX_CASES];

    public FitnessFunction(FitnessFunctionType functionType, double threshold) {
        this.functionType = functionType;
        this.threshold = threshold;

        for (int i = 1; i < MAX_CASES; i++) {
            cons[i] = ((double) 1.0) / i;
        }
    }

    public int getMinExpected() {
        return minExpected;
    }

    public void setMinExpected(int minExpected) {
        this.minExpected = minExpected;
    }

    public int getMinCases() {
        return minCases;
    }

    public void setMinCases(int minCases) {
        this.minCases = minCases;
    }

    public boolean isWorthTesting(double expected, double cases) {
        return ((expected <= cases) && (expected >= minExpected) && (cases >= minCases));
    }

    public double getStat(double expected, double cases) {
        double fit = Double.NaN;

        double[] cumPrb = new double[MAX_CASES];

        int jA = (int) cases;
        double aMean = (double) expected;

        if (jA > MAX_CASES) {
            LOGGER.log(Level.WARNING, "Too many cases for a Poisson Test");
        }

        double prob;
        if (jA > 1) {
            cumPrb[0] = Math.exp(-aMean);
            prob = cumPrb[0];
            for (int j = 1; j < jA; j++) {
                cumPrb[j] = aMean * cons[j] * cumPrb[j - 1];
                prob += cumPrb[j];
            }
            prob = 1.0 - prob;
        } else {
            prob = 1.0 - Math.exp(-aMean);
        }

        if (prob <= threshold) {
            switch (functionType) {
            case Poisson: // Poisson Probability fitness function
                fit = 1.0 - prob;
                break;
            case Relative: // Relative fitness function
                fit = cases - expected;
                break;
            case RelativePercent: // RelativePercent fitness function
                fit = cases / expected;
                break;
            }
            return fit;
        } else {
            return Double.NaN;
        }
    }
}