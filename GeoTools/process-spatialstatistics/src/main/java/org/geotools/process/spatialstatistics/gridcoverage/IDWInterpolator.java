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

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.gridcoverage.RasterRadius.SearchRadiusType;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * Implementation of Inverse Distance Weighted interpolation.
 * 
 * @author Minpa Lee, MangoSystem <br>
 * @see https://github.com/geotools/geotools/blob/master/spike/jan/gsoc-transformations/src/main/java/org/geotools/referencing/operation/builder/
 *      algorithm/IDWInterpolation.java
 * 
 * @source $URL$
 */
public class IDWInterpolator extends AbstractInterpolator {
    protected static final Logger LOGGER = Logging.getLogger(IDWInterpolator.class);

    private KdTree spatialIndex = new KdTree(0.0d);

    private RasterRadius radius = new RasterRadius();

    private double power = 2.0;

    public IDWInterpolator(Coordinate[] samples) {
        this(samples, new RasterRadius(), 2.0);
    }

    public IDWInterpolator(Coordinate[] samples, RasterRadius radius, double power) {
        this.samples = samples;
        this.number = samples.length;
        this.radius = radius;
        this.power = power;

        init();
    }

    private void init() {
        for (Coordinate sample : samples) {
            spatialIndex.insert(sample);
        }
    }

    @Override
    public double getValue(Coordinate p) {
        if (radius.getRadiusType() == SearchRadiusType.Fixed) {
            return interpolateFixed(p);
        } else {
            return interpolateVariable(p);
        }
    }

    /**
     * Variable search radius With a variable search radius, the number of points used in calculating the value of the interpolated cell is specified,
     * which makes the radius distance vary for each interpolated cell, depending on how far it has to search around each interpolated cell to reach
     * the specified number of input points
     * 
     * @param p locations
     * @return interpolated value
     */
    private double interpolateVariable(Coordinate p) {
        // List ret = spatialIndex.query(queryEnv);

        Map<Double, Coordinate> sortedMap = new TreeMap<Double, Coordinate>();
        for (Coordinate sample : samples) {
            final double distance = p.distance(sample);
            if (radius.distance > 0 && radius.distance < distance) {
                continue;
            } else {
                sortedMap.put(Double.valueOf(distance), sample);
            }
        }

        return interpolate(sortedMap);
    }

    /**
     * Fixed search radius <br>
     * When there are fewer measured points in the neighborhood than the specified minimum, <br>
     * the search radius will increase until it can encompass the minimum number of points
     * 
     * @param p locations
     * @return interpolated value
     */
    private double interpolateFixed(Coordinate p) {
        /* ============================================= */
        /* TODO Fix! */
        /* ============================================= */
        Map<Double, Coordinate> sortedMap = new TreeMap<Double, Coordinate>();
        for (Coordinate sample : samples) {
            final double distance = p.distance(sample);
            if (radius.numberOfPoints > 0) {
                sortedMap.put(Double.valueOf(distance), sample);
            } else {
                if (radius.distance >= distance) {
                    sortedMap.put(Double.valueOf(distance), sample);
                }
            }
        }

        return interpolate(sortedMap);
    }

    private double interpolate(Map<Double, Coordinate> sortedMap) {
        double sumWeight = 0;
        double weightSumDist = 0;
        int process = 0;
        for (Entry<Double, Coordinate> entry : sortedMap.entrySet()) {
            process++;
            final double weight = Math.pow(entry.getKey(), -power);
            weightSumDist += weight * entry.getValue().z;
            sumWeight += weight;
            if (radius.numberOfPoints > 0 && process >= radius.numberOfPoints) {
                break;
            }
        }

        return sumWeight > 0 ? weightSumDist / sumWeight : -Float.MAX_VALUE;
    }

}
