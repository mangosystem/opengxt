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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Eliminates sliver polygons by merging them with neighboring polygons that have the largest or smallest area or the longest shared border.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class EliminateOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(EliminateOperation.class);

    public enum EliminateOption {
        /*
         * The neighboring polygon is the one with the longest shared border.
         */
        Length,

        /*
         * The neighboring polygon is the one with the largest area.
         */
        LargeArea,

        /*
         * The neighboring polygon is the one with the smallest area.
         */
        SmallArea
    }

    private STRtree spatialIndex;

    private EliminateOption option = EliminateOption.Length;

    private Filter exception = Filter.EXCLUDE;

    public EliminateOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            EliminateOption option, Filter exception) throws IOException {
        this.exception = exception == null ? Filter.EXCLUDE : exception;
        this.option = option;

        buildSpatialIndex(inputFeatures);

        // prepare transactional feature store
        SimpleFeatureType featureType = inputFeatures.getSchema();
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            List<Geometry> neighbors = new ArrayList<Geometry>();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                if (this.exception.evaluate(feature)) {
                    // insert source feature
                    insertFeature(featureWriter, feature, true, null);
                    continue;
                }

                String id = feature.getID();

                Geometry source = (Geometry) feature.getDefaultGeometry();
                PreparedGeometry prepared = PreparedGeometryFactory.prepare(source);
                GeometryFactory factory = source.getFactory();

                // query
                neighbors.clear();
                for (@SuppressWarnings("unchecked")
                Iterator<NeighborFeature> iter = (Iterator<NeighborFeature>) spatialIndex.query(
                        source.getEnvelopeInternal()).iterator(); iter.hasNext();) {
                    NeighborFeature sample = iter.next();
                    if (sample.id.equals(id) || prepared.disjoint(sample.location)) {
                        continue;
                    }

                    // explode & only use exterior ring
                    for (int index = 0; index < sample.location.getNumGeometries(); index++) {
                        Polygon poly = (Polygon) sample.location.getGeometryN(index);
                        if (prepared.disjoint(poly)) {
                            continue;
                        }
                        LinearRing linearRing = (LinearRing) poly.getExteriorRing();
                        Geometry exterior = factory.createPolygon(linearRing, null);
                        neighbors.add(exterior);
                    }
                }

                if (neighbors.size() == 0) {
                    // insert source feature
                    insertFeature(featureWriter, feature, true, null);
                    continue;
                }

                // post process
                Geometry eliminated = eliminateSilver(source, neighbors);
                if (eliminated == null || eliminated.isEmpty()) {
                    continue;
                }

                // insert source feature
                insertFeature(featureWriter, feature, false, eliminated);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private Geometry eliminateSilver(Geometry source, List<Geometry> neighbors) {
        Geometry eliminated = source;

        PreparedGeometry prepared = PreparedGeometryFactory.prepare(source);
        GeometryFactory factory = source.getFactory();

        // find sliver polygons
        List<Geometry> slivers = new ArrayList<Geometry>();
        Geometry union = source.union(CascadedPolygonUnion.union(neighbors));
        for (int index = 0; index < union.getNumGeometries(); index++) {
            Polygon poly = (Polygon) union.getGeometryN(index);
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                LinearRing hole = (LinearRing) poly.getInteriorRingN(i);
                if (prepared.disjoint(hole)) {
                    continue;
                }
                slivers.add(factory.createPolygon((LinearRing) hole.reverse()));
            }
        }

        if (slivers.size() == 0) {
            return eliminated;
        }

        // build result
        List<Geometry> owns = new ArrayList<Geometry>();
        if (option == EliminateOption.Length) {
            for (Geometry sliver : slivers) {
                Geometry intersection = lineIntersection(source, sliver);
                double sourceLength = intersection.getLength();

                double targetLength = Double.MIN_VALUE;
                for (Geometry neighbor : neighbors) {
                    if (sliver.disjoint(neighbor)) {
                        continue;
                    }

                    Geometry neigoborIntersecton = lineIntersection(neighbor, sliver);
                    targetLength = Math.max(targetLength, neigoborIntersecton.getLength());
                }

                if (sourceLength > targetLength) {
                    owns.add(sliver);
                }
            }
        } else {
            double sourceArea = source.getArea();
            boolean largest = option == EliminateOption.LargeArea;

            List<Double> neighborAreas = new ArrayList<Double>();
            for (int index = 0; index < neighbors.size(); index++) {
                Geometry neighbor = neighbors.get(index);
                neighborAreas.add(neighbor.getArea());
            }

            for (Geometry sliver : slivers) {
                double targetArea = largest ? Double.MIN_VALUE : Double.MAX_VALUE;
                for (int index = 0; index < neighbors.size(); index++) {
                    Geometry neighbor = neighbors.get(index);
                    if (sliver.disjoint(neighbor)) {
                        continue;
                    }

                    if (largest) {
                        targetArea = Math.max(targetArea, neighborAreas.get(index));
                    } else {
                        targetArea = Math.min(targetArea, neighborAreas.get(index));
                    }
                }

                if (largest && sourceArea > targetArea) {
                    owns.add(sliver);
                } else if (!largest && sourceArea < targetArea) {
                    owns.add(sliver);
                }
            }
        }

        if (owns.size() > 0) {
            eliminated = source.union(CascadedPolygonUnion.union(owns));
        }

        return eliminated;
    }

    private Geometry lineIntersection(Geometry source, Geometry sliver) {
        final List<LineString> list = new ArrayList<LineString>();

        Geometry intersection = source.getBoundary().intersection(sliver.getBoundary());
        intersection.apply(new GeometryComponentFilter() {
            @Override
            public void filter(Geometry geom) {
                if (geom instanceof LineString) {
                    list.add((LineString) geom);
                } else if (geom instanceof MultiLineString) {
                    for (int i = 0; i < geom.getNumGeometries(); i++) {
                        list.add((LineString) geom.getGeometryN(i));
                    }
                }
            }
        });

        return source.getFactory().createMultiLineString(GeometryFactory.toLineStringArray(list));
    }

    private void insertFeature(IFeatureInserter featureWriter, SimpleFeature source,
            boolean copyGeometry, Geometry newGeometry) throws IOException {
        SimpleFeature newFeature = featureWriter.buildFeature();
        featureWriter.copyAttributes(source, newFeature, copyGeometry);
        if (newGeometry != null) {
            newFeature.setDefaultGeometry(newGeometry);
        }
        featureWriter.write(newFeature);
    }

    private void buildSpatialIndex(SimpleFeatureCollection features) {
        spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                NeighborFeature nearFeature = new NeighborFeature(geometry, feature.getID());
                spatialIndex.insert(geometry.getEnvelopeInternal(), nearFeature);
            }
        } finally {
            featureIter.close();
        }
    }

    static final class NeighborFeature {

        public Geometry location;

        public Object id;

        public NeighborFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }
}