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
package org.geotools.process.spatialstatistics.gridcoverage;

import org.geotools.referencing.operation.matrix.GeneralMatrix;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Implementation of 2D Thin Plate Spline interpolation.
 * <p>
 * http://en.wikipedia.org/wiki/Thin_plate_spline <br>
 * http://elonen.iki.fi/code/tpsdemo/index.html
 * 
 * @author Minpa Lee
 * @see https://github.com/geotools/geotools/blob/master/spike/jan/gsoc-transformations/src/main/java/org/geotools/referencing/operation/builder/algorithm/TPSInterpolation.java
 * @source $URL$
 */
public class ThinPlateSplineInterpolator extends AbstractInterpolator {

    /** Main matrix (according http://elonen.iki.fi/code/tpsdemo/index.html) */
    private GeneralMatrix L;

    /** Matrix of target values (according http://elonen.iki.fi/code/tpsdemo/index.html) */
    private GeneralMatrix V;

    private GeneralMatrix result;

    public ThinPlateSplineInterpolator(Coordinate[] samples) {
        this.samples = samples;
        this.number = samples.length;

        L = new GeneralMatrix(number + 3, number + 3);

        fillKsubMatrix();
        fillPsubMatrix();
        fillOsubMatrix();

        L.invert();

        GeneralMatrix V = fillVMatrix(0);
        result = new GeneralMatrix(number + 3, 1);
        result.mul(L, V);
    }

    @Override
    public double getValue(Coordinate p) {
        double a1 = result.getElement(result.getNumRow() - 3, 0);
        double a2 = result.getElement(result.getNumRow() - 2, 0);
        double a3 = result.getElement(result.getNumRow() - 1, 0);

        double sum = 0;
        for (int i = 0; i < (result.getNumRow() - 3); i++) {
            double dist = p.distance(samples[i]);
            sum += (result.getElement(i, 0) * functionU(dist));
        }

        return sum + a1 + (a2 * p.x) + (a3 * p.y);
    }

    /**
     * Calculates U function for distance
     * 
     * @param distance distance
     * @return log(distance)*distance<sub>2</sub> or 0 if distance = 0
     */
    private double functionU(double distance) {
        if (distance == 0) {
            return 0;
        }

        return distance * distance * Math.log(distance);
    }

    /**
     * Calculates U function where distance = ||p_i, p_j|| (from source points)
     * 
     * @param p_i p_i
     * @param p_j p_j
     * @return log(distance)*distance<sub>2</sub> or 0 if distance = 0
     */
    private double calculateFunctionU(Coordinate p_i, Coordinate p_j) {
        double distance = p_i.distance(p_j);

        return functionU(distance);
    }

    /**
     * Fill K submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     */
    private void fillKsubMatrix() {
        double alfa = 0;

        for (int i = 0; i < number; i++) {
            for (int j = i + 1; j < number; j++) {
                double u = calculateFunctionU(samples[i], samples[j]);
                L.setElement(i, j, u);
                L.setElement(j, i, u);
                alfa = alfa + (u * 2); // same for upper and lower part
            }
        }

        alfa = alfa / (number * number);
    }

    /**
     * Fill L submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     */
    private void fillPsubMatrix() {
        for (int i = 0; i < number; i++) {
            L.setElement(i, i, 0);

            Coordinate source = samples[i];

            L.setElement(i, number + 0, 1);
            L.setElement(i, number + 1, source.x);
            L.setElement(i, number + 2, source.y);

            L.setElement(number + 0, i, 1);
            L.setElement(number + 1, i, source.x);
            L.setElement(number + 2, i, source.y);
        }
    }

    /**
     * Fill O submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     */
    private void fillOsubMatrix() {
        for (int i = number; i < (number + 3); i++) {
            for (int j = number; j < (number + 3); j++) {
                L.setElement(i, j, 0);
            }
        }
    }

    /**
     * Fill V matrix (matrix of target values)
     * 
     * @param dim 0 for dx, 1 for dy.
     * @return V Matrix
     */
    private GeneralMatrix fillVMatrix(int dim) {
        V = new GeneralMatrix(number + 3, 1);

        for (int i = 0; i < number; i++) {
            V.setElement(i, 0, samples[i].z);
        }

        V.setElement(V.getNumRow() - 3, 0, 0);
        V.setElement(V.getNumRow() - 2, 0, 0);
        V.setElement(V.getNumRow() - 1, 0, 0);

        return V;
    }
}