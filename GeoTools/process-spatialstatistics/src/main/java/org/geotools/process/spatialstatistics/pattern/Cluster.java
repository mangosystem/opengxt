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

import org.opengis.feature.simple.SimpleFeature;

/**
 * Calculates a nearest neighbor index based on the average distance from each feature to its nearest neighboring feature.
 * 
 * @author Minpa Lee, MangoSystem
 * @reference http://code.google.com/p/hdict/source/browse/src/com/google/io/kmeans/Clusterer.java
 * @source $URL$
 */
public interface Cluster {
    /**
     * A class representing a 2D point. We don't use android.graphics.Point because that class changes its hash value based on its coordinates, where
     * this app needs to use an instance-based hash. http://code.google.com/p/hdict/source/browse/src/com/google/io/kmeans/Clusterer.java
     */
    public static class PointEvent {
        public int id;

        public double x;

        public double y;

        public double weight;

        public int cluster;

        public SimpleFeature feature;

        public PointEvent() {
        }

        public PointEvent(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public PointEvent(double x, double y, double weight) {
            this.x = x;
            this.y = y;
            this.weight = weight;
        }

        public PointEvent(double x, double y, double weight, SimpleFeature feature) {
            this.x = x;
            this.y = y;
            this.weight = weight;
            this.feature = feature;
        }

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void setCluster(int cluster) {
            this.cluster = cluster;
        }
    }

    /**
     * Runs a k-means clustering pass on the indicated data.
     * 
     * @param numClusters The number of clusters to group the points into
     */
    public PointEvent[] cluster(int numClusters);
}
