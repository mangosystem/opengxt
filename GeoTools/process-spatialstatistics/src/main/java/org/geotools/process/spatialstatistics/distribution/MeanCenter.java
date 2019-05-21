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

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * MeanCenter
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MeanCenter {
    double sumX = 0.0;

    double sumY = 0.0;

    double sumZ = 0.0;

    double weightSum = 0.0;

    double dimensionSum = 0.0;

    int numFeatures = 0;

    GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public void addValue(Coordinate coordinate, double weight, double dimVal) {
        weightSum += weight;
        sumX += coordinate.x * weight;
        sumY += coordinate.y * weight;
        sumZ += coordinate.z * weight;

        if (!Double.isNaN(dimVal) && !Double.isInfinite(dimVal)) {
            dimensionSum += dimVal;
        }

        numFeatures++;
    }

    public Point getMeanCenter() {
        double meanX = sumX / weightSum;
        double meanY = sumY / weightSum;
        double meanZ = sumZ / weightSum;

        return gf.createPoint(new Coordinate(meanX, meanY, meanZ));
    }

    public double getDimension() {
        return dimensionSum / numFeatures;
    }
}
