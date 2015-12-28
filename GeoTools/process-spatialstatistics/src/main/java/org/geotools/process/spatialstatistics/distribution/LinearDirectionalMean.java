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
package org.geotools.process.spatialstatistics.distribution;

import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.SSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * LinearDirectionalMean
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LinearDirectionalMean {

    private double sumX = 0.0;

    private double sumY = 0.0;

    private double sumZ = 0.0;

    private double sumSin = 0.0;

    private double sumCos = 0.0;

    private double sumLen = 0.0;

    private int numFeatures = 0;

    private boolean orientationOnly = false;

    public void setOrientationOnly(boolean orientationOnly) {
        this.orientationOnly = orientationOnly;
    }

    public void addValue(Geometry lineString) {
        if (lineString instanceof LineString) {
            insert((LineString) lineString);
        } else if (lineString instanceof MultiLineString) {
            for (int index = 0; index < lineString.getNumGeometries(); index++) {
                insert((LineString) lineString.getGeometryN(index));
            }
        }
    }

    private void insert(LineString lineString) {
        Coordinate centroid = lineString.getCentroid().getCoordinate();
        sumX += centroid.x * 1.0;
        sumY += centroid.y * 1.0;
        sumZ += centroid.z * 1.0;

        // Get Angle
        Point firstPoint = lineString.getStartPoint();
        Point lastPoint = lineString.getEndPoint();
        double angle = SSUtils.getAngle(firstPoint, lastPoint);

        // Adjust for Orientation Only
        if (orientationOnly) {
            double angle2Degree = SSUtils.convert2Degree(angle);
            if (angle2Degree < 180) {
                angle = SSUtils.getAngle(lastPoint, firstPoint);
            }
        }

        sumLen += lineString.getLength();
        sumSin += Math.sin(angle);
        sumCos += Math.cos(angle);

        numFeatures++;
    }

    public LineString getDirectionalLine() {
        // Get Start and End Points
        double halfMeanLen = getMeanLength() / 2.0;
        double meanX = getMeanX();
        double meanY = getMeanY();
        double radianAngle = getRadianAngle();

        double fromX = (halfMeanLen * Math.sin(radianAngle)) + meanX;
        double toX = (2.0 * meanX) - fromX;

        double fromY = (halfMeanLen * Math.cos(radianAngle)) + meanY;
        double toY = (2.0 * meanY) - fromY;

        double fromZ = 0.0;
        double toZ = 0.0;

        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

        Coordinate toCoord = new Coordinate(toX, toY, toZ);
        Coordinate fromCoord = new Coordinate(fromX, fromY, fromZ);

        return gf.createLineString(new Coordinate[] { fromCoord, toCoord });
    }

    public double getCirVar() {
        double unstandardized = Math.sqrt(Math.pow(sumSin, 2.0) + Math.pow(sumCos, 2.0));
        return 1.0 - (unstandardized / (numFeatures * 1.0));
    }

    public double getDirMean() {
        double degreeAngle = getDegreeAngle();
        if (orientationOnly) {
            degreeAngle = degreeAngle - 180.0;
        }

        double dirMean = 360.0 - degreeAngle + 90.0;
        if (dirMean >= 360) {
            dirMean = dirMean - 360.0;
        }

        return dirMean;
    }

    public double getSumSin() {
        return sumSin;
    }

    public double getRadianAngle() {
        return SSUtils.getAngle(sumSin, sumCos);
    }

    public double getDegreeAngle() {
        return SSUtils.convert2Degree(getRadianAngle());
    }

    public double getSumCos() {
        return sumCos;
    }

    public double getMeanLength() {
        return sumLen / numFeatures;
    }

    public double getMeanX() {
        return sumX / numFeatures;
    }

    public double getMeanY() {
        return sumY / numFeatures;
    }

    public double getMeanZ() {
        return sumZ / numFeatures;
    }
}