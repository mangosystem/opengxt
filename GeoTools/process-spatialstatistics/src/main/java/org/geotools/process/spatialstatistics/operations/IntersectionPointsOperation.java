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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Creates point features where the lines in the input features intersect the lines in the intersect features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class IntersectionPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(IntersectionPointsOperation.class);

    static final String TYPE_NAME = "IntersectionPoints";

    private boolean hasIntersectID = false;

    private STRtree spatialIndex;

    public IntersectionPointsOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection intersectFeatures, String intersectIDField) throws IOException {
        SimpleFeatureType inputSchema = inputFeatures.getSchema();
        SimpleFeatureType interSchema = intersectFeatures.getSchema();

        // build schema
        SimpleFeatureType schema = FeatureTypes.build(inputSchema, TYPE_NAME, Point.class);
        if (intersectIDField != null && interSchema.indexOf(intersectIDField) != -1) {
            schema = FeatureTypes.add(schema, interSchema.getDescriptor(intersectIDField));
            hasIntersectID = true;
        }

        // check coordinate reference system
        CoordinateReferenceSystem crsT = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = intersectFeatures.getSchema()
                .getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            intersectFeatures = new ReprojectFeatureCollection(intersectFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // build spatial index
        this.buildSpatialIndex(intersectFeatures, intersectIDField);

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            final Map<Geometry, Object> points = new HashMap<Geometry, Object>();
            boolean isPolygon = FeatureTypes.getSimpleShapeType(inputFeatures) == SimpleShapeType.POLYGON;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (isPolygon) {
                    geometry = geometry.getBoundary();
                }

                PreparedGeometry prepared = PreparedGeometryFactory.prepare(geometry);
                points.clear();

                // get intersection points, duplicated points should be removed!
                for (@SuppressWarnings("unchecked")
                Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(
                        geometry.getEnvelopeInternal()).iterator(); iter.hasNext();) {
                    final NearFeature sample = iter.next();
                    if (prepared.intersects(sample.location)) {
                        Geometry intersections = geometry.intersection(sample.location);
                        intersections.apply(new GeometryComponentFilter() {
                            @Override
                            public void filter(Geometry geom) {
                                if (geom instanceof Point) {
                                    points.put(geom, sample.id);
                                } else if (geom instanceof MultiPoint) {
                                    for (int idx = 0; idx < geom.getNumGeometries(); idx++) {
                                        points.put(geom.getGeometryN(idx), sample.id);
                                    }
                                }
                            }
                        });
                    }
                }

                // insert geometries
                if (points.size() == 0) {
                    continue;
                }

                // insert features
                for (Entry<Geometry, Object> entry : points.entrySet()) {
                    // create & insert feature
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    featureWriter.copyAttributes(feature, newFeature, false);
                    newFeature.setDefaultGeometry(entry.getKey());

                    if (hasIntersectID) {
                        newFeature.setAttribute(intersectIDField, entry.getValue());
                    }

                    featureWriter.write(newFeature);
                }
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private void buildSpatialIndex(SimpleFeatureCollection features, String idField) {
        spatialIndex = new STRtree();
        boolean isPolygon = FeatureTypes.getSimpleShapeType(features) == SimpleShapeType.POLYGON;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (isPolygon) {
                    geometry = geometry.getBoundary();
                }

                Object featureID = feature.getID();
                if (hasIntersectID) {
                    featureID = feature.getAttribute(idField);
                }

                NearFeature near = new NearFeature(geometry, featureID);
                spatialIndex.insert(geometry.getEnvelopeInternal(), near);
            }
        } finally {
            featureIter.close();
        }
    }

    static final class NearFeature {

        public Geometry location;

        public Object id;

        public NearFeature(Geometry location, Object id) {
            this.location = location;
            this.id = id;
        }
    }

}
