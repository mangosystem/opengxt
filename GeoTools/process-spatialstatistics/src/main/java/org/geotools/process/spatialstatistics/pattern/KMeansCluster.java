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
package org.geotools.process.spatialstatistics.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * k-means clustering
 * 
 * @author Minpa Lee, MangoSystem
 * @reference http://code.google.com/p/hdict/source/browse/src/com/google/io/kmeans/DalvikClusterer.java
 * @source $URL$
 */
public class KMeansCluster implements Cluster {
    protected static final Logger LOGGER = Logging.getLogger(KMeansCluster.class);

    static final int MAX_LOOP_COUNT = 100;

    final Random random = new Random(System.currentTimeMillis());

    private PointEvent[] points = null;

    private ReferencedEnvelope extent = null;

    public PointEvent[] getPoints() {
        return this.points;
    }

    public KMeansCluster(SimpleFeatureCollection pointFeatures) {
        loadPoints(pointFeatures, null);
    }

    private PointEvent getRandomPoint() {
        PointEvent randomPoint = new PointEvent();

        while (true) {
            double x = extent.getMinX() + random.nextInt((int) extent.getWidth());
            double y = extent.getMinY() + random.nextInt((int) extent.getHeight());
            if (extent.contains(x, y)) {
                randomPoint.x = x;
                randomPoint.y = y;
                break;
            }
        }

        return randomPoint;
    }

    @Override
    public PointEvent[] cluster(int numClusters) {
        boolean converged = false;
        boolean dirty;
        double distance;
        double curMinDistance;
        int loopCount = 0;
        PointEvent point;

        // randomly pick some points to be the centroids of the groups, for the first pass
        PointEvent[] means = new PointEvent[numClusters];
        for (int i = 0; i < numClusters; ++i) {
            means[i] = getRandomPoint();
            means[i].cluster = i;
        }

        // initialize data
        double[] distances = new double[points.length];
        Arrays.fill(distances, Double.MAX_VALUE);

        double[] sumX = new double[numClusters];
        double[] sumY = new double[numClusters];
        int[] clusterSizes = new int[numClusters];

        // main loop
        while (!converged) {
            dirty = false;

            // compute which group each point is closest to
            for (int i = 0; i < points.length; ++i) {
                point = points[i];
                curMinDistance = distances[i];
                for (PointEvent mean : means) {
                    distance = computeDistance(point, mean);
                    if (distance < curMinDistance) {
                        dirty = true;
                        distances[i] = distance;
                        curMinDistance = distance;
                        point.cluster = mean.cluster;
                    }
                }
            }

            // if we did no work, break early (greedy algorithm has converged)
            if (!dirty) {
                converged = true;
                break;
            }

            // compute the new centroids of the groups, since contents have changed
            for (int i = 0; i < numClusters; ++i) {
                sumX[i] = sumY[i] = clusterSizes[i] = 0;
            }

            for (int i = 0; i < points.length; ++i) {
                point = points[i];
                sumX[point.cluster] += point.x;
                sumY[point.cluster] += point.y;
                clusterSizes[point.cluster] += 1;
            }

            for (int i = 0; i < numClusters; ++i) {
                try {
                    means[i].x = sumX[i] / clusterSizes[i];
                    means[i].y = sumY[i] / clusterSizes[i];
                } catch (ArithmeticException e) {
                    // means a Divide-By-Zero error, b/c no points were associated with this
                    // cluster.
                    // rare, so reset the cluster to have a new random center
                    PointEvent p = getRandomPoint();
                    means[i].x = p.x;
                    means[i].y = p.y;
                }
            }

            // bail out after at most MAX_LOOP_COUNT passes
            loopCount++;
            converged = converged || (loopCount > MAX_LOOP_COUNT);
        }

        return means;
    }

    /**
     * Computes the Cartesian distance between two points.
     */
    private double computeDistance(PointEvent a, PointEvent b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private void loadPoints(SimpleFeatureCollection pointFeatures, String weightField) {
        this.extent = pointFeatures.getBounds();

        List<Cluster.PointEvent> ptList = new ArrayList<Cluster.PointEvent>();
        SimpleFeatureIterator featureIter = featureIter = pointFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Point coord = geometry.getCentroid();
                ptList.add(new PointEvent(coord.getX(), coord.getY(), 1.0, feature));
            }
        } finally {
            featureIter.close();
        }

        points = new PointEvent[ptList.size()];
        ptList.toArray(points);
    }
}
