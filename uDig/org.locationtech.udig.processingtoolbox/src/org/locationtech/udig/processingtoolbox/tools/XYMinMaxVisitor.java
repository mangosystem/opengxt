/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * XY MinMax Visitor.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
class XYMinMaxVisitor {
    private double minX = Double.MAX_VALUE;

    private double maxX = Double.MIN_VALUE;

    private double minY = Double.MAX_VALUE;

    private double maxY = Double.MIN_VALUE;

    private double minZ = Double.MAX_VALUE;

    private double maxZ = Double.MIN_VALUE;

    private double sumX = 0d;

    private double sumY = 0d;

    private double sumZ = 0d;

    private int count = 0;

    public void reset() {
        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;
        minZ = Double.MAX_VALUE;
        maxZ = Double.MIN_VALUE;
        sumX = 0d;
        sumY = 0d;
        sumZ = 0d;
        count = 0;
    }

    public void visit(double x, double y) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);

        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);

        sumX += x;
        sumY += y;
        count++;
    }

    public void visit(double x, double y, double z) {
        visit(x, y);

        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
    }

    public void visit(SimpleFeatureCollection features, String xField, String yField, String zField) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Expression xExpression = ff.property(xField);
        Expression yExpression = ff.property(yField);
        Expression zExpression = ff.property(zField);

        reset();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                Double xVal = xExpression.evaluate(feature, Double.class);
                if (xVal == null || xVal.isNaN() || xVal.isInfinite()) {
                    continue;
                }

                Double yVal = yExpression.evaluate(feature, Double.class);
                if (yVal == null || yVal.isNaN() || yVal.isInfinite()) {
                    continue;
                }

                Double zVal = zExpression.evaluate(feature, Double.class);
                if (zVal == null || zVal.isNaN() || zVal.isInfinite()) {
                    continue;
                }

                visit(xVal, yVal, zVal);
            }
        } finally {
            featureIter.close();
        }
    }

    public double getAverageX() {
        return sumX / count;
    }

    public double getAverageY() {
        return sumY / count;
    }

    public double getAverageZ() {
        return sumZ / count;
    }

    public double getAbsMaxX() {
        return Math.max(Math.abs(minX), Math.abs(maxX));
    }

    public double getAbsMaxY() {
        return Math.max(Math.abs(minY), Math.abs(maxY));
    }

    public double getAbsMaxZ() {
        return Math.max(Math.abs(minZ), Math.abs(maxZ));
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxZ() {
        return maxZ;
    }
}
