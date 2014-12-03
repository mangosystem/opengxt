package org.locationtech.udig.processingtoolbox.tools;

public class XYMinMaxVisitor {
    private double minX = Double.MAX_VALUE;

    private double maxX = Double.MIN_VALUE;

    private double minY = Double.MAX_VALUE;

    private double maxY = Double.MIN_VALUE;

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
