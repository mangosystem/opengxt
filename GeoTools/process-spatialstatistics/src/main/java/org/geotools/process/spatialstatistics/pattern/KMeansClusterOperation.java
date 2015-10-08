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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.pattern.Cluster.PointEvent;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * k-means clustering
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KMeansClusterOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(KMeansClusterOperation.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String targetField,
            int numClusters) throws IOException {
        KMeansCluster cluster = new KMeansCluster(features);
        cluster.cluster(numClusters);
        PointEvent[] originPoints = cluster.getPoints();

        SimpleFeatureType featureType = FeatureTypes.build(features, this.getOutputTypeName());
        featureType = FeatureTypes.add(featureType, targetField, Integer.class, 10);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        try {
            for (PointEvent cp : originPoints) {
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(cp.feature, newFeature, true);
                newFeature.setAttribute(targetField, cp.cluster);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection executeAsCircle(SimpleFeatureCollection features,
            String targetField, int numClusters) throws IOException {
        KMeansCluster cluster = new KMeansCluster(features);
        cluster.cluster(numClusters);
        PointEvent[] originPoints = cluster.getPoints();

        Hashtable<Integer, List<Geometry>> clusters = new Hashtable<Integer, List<Geometry>>();
        for (PointEvent cp : originPoints) {
            final Integer clusterID = Integer.valueOf(cp.cluster);
            if (!clusters.containsKey(clusterID)) {
                clusters.put(clusterID, new ArrayList<Geometry>());
            }
            clusters.get(clusterID).add((Geometry) cp.feature.getDefaultGeometry());
        }

        CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
        String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(this.getOutputTypeName(),
                the_geom, Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, targetField, Integer.class, 10);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        try {
            for (Entry<Integer, List<Geometry>> entry : clusters.entrySet()) {
                if (entry.getValue().size() == 0) {
                    continue;
                }
                Geometry[] geometries = GeometryFactory.toGeometryArray(entry.getValue());
                GeometryCollection geomCol = gf.createGeometryCollection(geometries);
                Geometry circle = new MinimumBoundingCircle(geomCol).getCircle();

                SimpleFeature newFeature = featureWriter.buildFeature(null);
                newFeature.setDefaultGeometry(circle);
                newFeature.setAttribute(targetField, entry.getKey());
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection makeCircle(SimpleFeatureCollection clusterFeatures,
            String clusterField) throws IOException {
        Hashtable<Integer, List<Geometry>> clusters = new Hashtable<Integer, List<Geometry>>();
        final Expression exp = ff.property(clusterField);

        SimpleFeatureIterator featureIter = clusterFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Integer clusterID = exp.evaluate(feature, Integer.class);
                if (clusterID == null) {
                    continue;
                }
                if (!clusters.containsKey(clusterID)) {
                    clusters.put(clusterID, new ArrayList<Geometry>());
                }
                clusters.get(clusterID).add((Geometry) feature.getDefaultGeometry());
            }
        } finally {
            featureIter.close();
        }

        CoordinateReferenceSystem crs = clusterFeatures.getSchema().getCoordinateReferenceSystem();
        String the_geom = clusterFeatures.getSchema().getGeometryDescriptor().getLocalName();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(this.getOutputTypeName(),
                the_geom, Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, clusterField, Integer.class, 10);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        try {
            for (Entry<Integer, List<Geometry>> entry : clusters.entrySet()) {
                if (entry.getValue().size() == 0) {
                    continue;
                }
                Geometry[] geometries = GeometryFactory.toGeometryArray(entry.getValue());
                GeometryCollection geomCol = gf.createGeometryCollection(geometries);
                Geometry circle = new MinimumBoundingCircle(geomCol).getCircle();

                SimpleFeature newFeature = featureWriter.buildFeature(null);
                newFeature.setDefaultGeometry(circle);
                newFeature.setAttribute(clusterField, entry.getKey());
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
