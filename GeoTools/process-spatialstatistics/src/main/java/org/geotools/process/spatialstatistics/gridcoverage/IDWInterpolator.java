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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.gridcoverage.RasterRadius.SearchRadiusType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * Implementation of Inverse Distance Weighted interpolation.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IDWInterpolator extends AbstractInterpolator {
    protected static final Logger LOGGER = Logging.getLogger(IDWInterpolator.class);

    private STRtree spatialIndex = null;

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
        spatialIndex = new STRtree();
        for (Coordinate sample : samples) {
            spatialIndex.insert(new Envelope(sample), sample);
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
        Map<Double, Coordinate> sortedMap = new TreeMap<Double, Coordinate>();

        Envelope searchEnv = new Envelope(p);
        searchEnv.expandBy(radius.distance);

        for (@SuppressWarnings("unchecked")
        Iterator<Coordinate> iter = (Iterator<Coordinate>) spatialIndex.query(searchEnv).iterator(); iter
                .hasNext();) {
            Coordinate sample = iter.next();
            double distance = p.distance(sample);
            if (radius.distance < distance) {
                continue;
            }
            sortedMap.put(Double.valueOf(distance), sample);
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
        Map<Double, Coordinate> sortedMap = new TreeMap<Double, Coordinate>();

        Envelope searchEnv = new Envelope(p);
        searchEnv.expandBy(radius.distance);

        if (radius.numberOfPoints > 0) {
            Object[] knns = spatialIndex.nearestNeighbour(searchEnv, p, new ItemDistance() {
                @Override
                public double distance(ItemBoundable item1, ItemBoundable item2) {
                    Coordinate s1 = (Coordinate) item1.getItem();
                    Coordinate s2 = (Coordinate) item2.getItem();
                    return s1.distance(s2);
                }
            }, radius.numberOfPoints);

            for (Object object : knns) {
                Coordinate sample = (Coordinate) object;
                sortedMap.put(Double.valueOf(p.distance(sample)), sample);
            }
        } else {
            for (@SuppressWarnings("unchecked")
            Iterator<Coordinate> iter = (Iterator<Coordinate>) spatialIndex.query(searchEnv)
                    .iterator(); iter.hasNext();) {
                Coordinate sample = iter.next();
                double distance = p.distance(sample);
                if (radius.distance < distance) {
                    continue;
                }
                sortedMap.put(Double.valueOf(distance), sample);
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
