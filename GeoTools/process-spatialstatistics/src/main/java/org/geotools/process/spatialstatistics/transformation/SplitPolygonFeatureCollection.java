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
package org.geotools.process.spatialstatistics.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

/**
 * Splits polygon features based on count or count expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitPolygonFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(SplitPolygonFeatureCollection.class);

    private Expression countExpression;

    public SplitPolygonFeatureCollection(SimpleFeatureCollection delegate, double desiredArea)
            throws CQLException {
        super(delegate);

        String geom = delegate.getSchema().getGeometryDescriptor().getLocalName();
        String cql = String.format("ceil(area(%s) / %s)", geom, desiredArea);
        this.countExpression = ECQL.toExpression(cql);
    }

    public SplitPolygonFeatureCollection(SimpleFeatureCollection delegate, int count) {
        this(delegate, ff.literal(count));
    }

    public SplitPolygonFeatureCollection(SimpleFeatureCollection delegate,
            Expression countExpression) {
        super(delegate);

        this.countExpression = countExpression;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SplitByDistanceFeatureIterator(delegate.features(), getSchema(), countExpression);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public int size() {
        return DataUtilities.count(features());
    }

    static class SplitByDistanceFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private PolygonSplitter spliter = new PolygonSplitter();

        private List<Geometry> subPolygons;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private Expression countExpression;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        private String typeName;

        public SplitByDistanceFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression countExpression) {
            this.delegate = delegate;

            this.index = 0;
            this.builder = new SimpleFeatureBuilder(schema);
            this.countExpression = countExpression;
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((nextFeature == null && delegate.hasNext())
                    || (nextFeature == null && !delegate.hasNext() && index > 0)) {
                if (index == 0) {
                    origFeature = delegate.next();
                    Double interval = countExpression.evaluate(origFeature, Double.class);

                    int count = interval == null ? 1 : interval.intValue();
                    Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                    subPolygons = spliter.divideByCount(geometry, count);
                }

                // create feature
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = subPolygons.get(index);
                    }
                    builder.add(attribute);
                }

                nextFeature = builder.buildFeature(buildID(typeName, ++featureID));
                builder.reset();
                index++;

                if (index >= subPolygons.size()) {
                    index = 0;
                    origFeature = null;
                    subPolygons.clear();
                }
            }
            return nextFeature != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = nextFeature;
            nextFeature = null;
            return result;
        }
    }

    static final class PolygonSplitter {
        private RandomPointsBuilder builder;

        static final int RANDOM_POINTS = 2000;

        static final int MAX_LOOP_COUNT = 200;

        public List<Geometry> divideByArea(Geometry sourcePolygon, double desiredArea) {
            int count = (int) Math.ceil(sourcePolygon.getArea() / desiredArea);
            return divideByCount(sourcePolygon, count);
        }

        // http://blog.cleverelephant.ca/2018/06/polygon-splitting.html
        public List<Geometry> divideByCount(Geometry sourcePolygon, int count) {
            List<Geometry> result = new ArrayList<Geometry>();
            if (count <= 1) {
                result.add(sourcePolygon);
                return result;
            }

            GeometryFactory gf = sourcePolygon.getFactory();

            // Convert a polygon to a set of points proportional to the area by ST_GeneratePoints
            // (the more points, the more beautiful it will be, guess 1000 is ok);
            builder = new RandomPointsBuilder(gf);
            builder.setExtent(sourcePolygon);
            builder.setNumPoints(RANDOM_POINTS);

            // Decide how many parts you’d like to split into, (ST_Area(geom) / max_area), let it be K;
            Geometry multiPoints = builder.getGeometry();
            Coordinate[] coordinates = new Coordinate[multiPoints.getNumGeometries()];
            for (int i = 0; i < multiPoints.getNumGeometries(); i++) {
                Point point = (Point) multiPoints.getGeometryN(i);
                coordinates[i] = point.getCoordinate();
            }

            // Take KMeans of the point cloud with K clusters;
            // For each cluster, take a ST_Centroid(ST_Collect(point));
            Coordinate[] means = k_means_cluster(coordinates, count);

            // Feed these centroids into ST_VoronoiPolygons, that will get you a mask for each part of polygon;
            VoronoiDiagramBuilder vdBuilder = new VoronoiDiagramBuilder();
            vdBuilder.setTolerance(0d);
            vdBuilder.setClipEnvelope(sourcePolygon.getEnvelopeInternal());
            vdBuilder.setSites(Arrays.asList(means));

            // ST_Intersection of original polygon and each cell of Voronoi polygons will get you a good split of your polygon into K parts.
            Geometry voronoiPolygons = vdBuilder.getDiagram(gf);
            for (int k = 0; k < voronoiPolygons.getNumGeometries(); k++) {
                Geometry voronoiPolygon = voronoiPolygons.getGeometryN(k);
                Geometry divided = sourcePolygon.intersection(voronoiPolygon);
                result.add(divided);
            }

            return result;
        }

        // http://code.google.com/p/hdict/source/browse/src/com/google/io/kmeans/DalvikClusterer.java
        private Coordinate[] k_means_cluster(Coordinate[] coordinates, int numClusters) {
            boolean converged = false;
            boolean dirty;
            double distance;
            double curMinDistance;
            int loopCount = 0;

            // randomly pick some points to be the centroids of the groups, for the first pass
            builder.setNumPoints(numClusters);
            Geometry multiPoints = builder.getGeometry();

            Coordinate[] means = new Coordinate[numClusters];
            for (int i = 0; i < numClusters; ++i) {
                Point point = (Point) multiPoints.getGeometryN(i);
                means[i] = point.getCoordinate();
                means[i].z = i;
            }

            // initialize data
            double[] distances = new double[coordinates.length];
            Arrays.fill(distances, Double.MAX_VALUE);

            double[] sumX = new double[numClusters];
            double[] sumY = new double[numClusters];
            int[] clusterSizes = new int[numClusters];

            // main loop
            while (!converged) {
                dirty = false;

                // compute which group each point is closest to
                for (int i = 0; i < coordinates.length; ++i) {
                    curMinDistance = distances[i];
                    for (Coordinate mean : means) {
                        distance = coordinates[i].distance(mean);
                        if (distance < curMinDistance) {
                            dirty = true;
                            distances[i] = distance;
                            curMinDistance = distance;
                            coordinates[i].z = mean.z;
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

                for (int i = 0; i < coordinates.length; ++i) {
                    sumX[(int) coordinates[i].z] += coordinates[i].x;
                    sumY[(int) coordinates[i].z] += coordinates[i].y;
                    clusterSizes[(int) coordinates[i].z] += 1;
                }

                for (int i = 0; i < numClusters; ++i) {
                    try {
                        means[i].x = sumX[i] / clusterSizes[i];
                        means[i].y = sumY[i] / clusterSizes[i];
                    } catch (ArithmeticException e) {
                        // means a Divide-By-Zero error, b/c no points were associated with this cluster.
                        // rare, so reset the cluster to have a new random center
                        Random rand = new Random(System.currentTimeMillis());
                        int rndIndex = rand.nextInt(coordinates.length);
                        Coordinate p = coordinates[rndIndex];
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
    }
}
