/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

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

    private double sumX = 0d;

    private double sumY = 0d;

    private int count = 0;

    public void reset() {
        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;
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

    public double getAverageX() {
        return sumX / count;
    }

    public double getAverageY() {
        return sumY / count;
    }

    public double getAbsMaxX() {
        return Math.max(Math.abs(minX), Math.abs(maxX));
    }

    public double getAbsMaxY() {
        return Math.max(Math.abs(minY), Math.abs(maxY));
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
}
