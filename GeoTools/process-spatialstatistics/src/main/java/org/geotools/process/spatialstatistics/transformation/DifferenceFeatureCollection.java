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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

/**
 * Difference SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DifferenceFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(DifferenceFeatureCollection.class);

    private SimpleFeatureCollection differenceFeatures;

    private SimpleFeatureType targetSchema;

    public DifferenceFeatureCollection(SimpleFeatureCollection delegate,
            SimpleFeatureCollection differenceFeatures) {
        super(delegate);

        // check coordinate reference system
        CoordinateReferenceSystem crsT = delegate.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = differenceFeatures.getSchema()
                .getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            differenceFeatures = new ReprojectFeatureCollection(differenceFeatures, crsS, crsT,
                    true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // use SpatialIndexFeatureCollection
        this.differenceFeatures = DataUtils.toSpatialIndexFeatureCollection(differenceFeatures);
        this.targetSchema = buildTargetSchema(delegate.getSchema());
    }

    private SimpleFeatureType buildTargetSchema(SimpleFeatureType schema) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                Class<?> binding = ad.getType().getBinding();
                if (Point.class.isAssignableFrom(binding)
                        || GeometryCollection.class.isAssignableFrom(binding)) {
                    tb.add(ad);
                } else {
                    Class<?> target;
                    if (LineString.class.isAssignableFrom(binding)) {
                        target = MultiLineString.class;
                    } else if (Polygon.class.isAssignableFrom(binding)) {
                        target = MultiPolygon.class;
                    } else {
                        throw new RuntimeException("Don't know how to handle geometries of type "
                                + binding.getCanonicalName());
                    }
                    tb.minOccurs(ad.getMinOccurs());
                    tb.maxOccurs(ad.getMaxOccurs());
                    tb.nillable(ad.isNillable());
                    tb.add(ad.getLocalName(), target, gd.getCoordinateReferenceSystem());
                }
            } else {
                tb.add(ad);
            }
        }
        tb.setName(schema.getName());
        return tb.buildFeatureType();
    }

    public SimpleFeatureType getSchema() {
        return targetSchema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new DifferenceFeatureIterator(delegate.features(), getSchema(), differenceFeatures);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    public int size() {
        return DataUtilities.count(features());
    }

    static class DifferenceFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private SimpleFeatureCollection differenceFeatures;

        private ReferencedEnvelope bounds;

        private String geomField;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private Class<?> target;

        public DifferenceFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                SimpleFeatureCollection differenceFeatures) {
            this.delegate = delegate;
            this.differenceFeatures = differenceFeatures;
            this.bounds = differenceFeatures.getBounds();
            this.geomField = differenceFeatures.getSchema().getGeometryDescriptor().getLocalName();
            this.builder = new SimpleFeatureBuilder(schema);
            this.target = schema.getGeometryDescriptor().getType().getBinding();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature feature = delegate.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Geometry diffGeom = geometry; // default
                if (bounds.intersects(geometry.getEnvelopeInternal())) {
                    Filter filter = getIntersectsFilter(geomField, geometry);

                    // finally difference using union geometries(intersection features)
                    List<Geometry> geometries = new ArrayList<Geometry>();
                    SimpleFeatureIterator difIter = differenceFeatures.subCollection(filter)
                            .features();
                    try {
                        while (difIter.hasNext()) {
                            SimpleFeature diffFeature = difIter.next();
                            geometries.add((Geometry) diffFeature.getDefaultGeometry());
                        }
                    } finally {
                        difIter.close();
                    }

                    if (geometries.size() > 0) {
                        Geometry unionGeometry = new CascadedPolygonUnion(geometries).union();
                        if (unionGeometry != null && !unionGeometry.isEmpty()) {
                            diffGeom = difference(geometry, unionGeometry, target);
                        }

                        if (diffGeom == null || diffGeom.isEmpty()) {
                            continue;
                        }
                    }
                }

                for (Object attribute : feature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        builder.add(diffGeom);
                    } else {
                        builder.add(attribute);
                    }
                }
                next = builder.buildFeature(feature.getID());
            }

            return next != null;
        }

        private Geometry difference(Geometry geom, Geometry overlay, Class<?> target) {
            Geometry difference = geom.difference(overlay);

            // empty difference?
            if (difference == null || difference.getNumGeometries() == 0) {
                return null;
            }

            // map the result to the target output type, removing the spurious lower dimensional
            // elements that might result out of the intersection
            Geometry result;
            if (Point.class.isAssignableFrom(target) || MultiPoint.class.isAssignableFrom(target)
                    || GeometryCollection.class.equals(target)) {
                result = difference;
            } else if (MultiLineString.class.isAssignableFrom(target)
                    || LineString.class.isAssignableFrom(target)) {
                final List<LineString> geoms = new ArrayList<LineString>();
                difference.apply(new GeometryComponentFilter() {

                    @Override
                    public void filter(Geometry geom) {
                        if (geom instanceof LineString) {
                            geoms.add((LineString) geom);
                        }
                    }
                });
                if (geoms.size() == 0) {
                    result = null;
                } else {
                    LineString[] ls = (LineString[]) geoms.toArray(new LineString[geoms.size()]);
                    result = geom.getFactory().createMultiLineString(ls);
                }
            } else if (MultiPolygon.class.isAssignableFrom(target)
                    || Polygon.class.isAssignableFrom(target)) {
                final List<Polygon> geoms = new ArrayList<Polygon>();
                difference.apply(new GeometryComponentFilter() {

                    @Override
                    public void filter(Geometry geom) {
                        if (geom instanceof Polygon) {
                            geoms.add((Polygon) geom);
                        }
                    }
                });
                if (geoms.size() == 0) {
                    result = null;
                } else {
                    Polygon[] ps = (Polygon[]) geoms.toArray(new Polygon[geoms.size()]);
                    result = geom.getFactory().createMultiPolygon(ps);
                }
            } else {
                throw new RuntimeException("Unrecognized target type " + target.getCanonicalName());
            }

            return result;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature result = next;
            next = null;
            return result;
        }
    }
}
