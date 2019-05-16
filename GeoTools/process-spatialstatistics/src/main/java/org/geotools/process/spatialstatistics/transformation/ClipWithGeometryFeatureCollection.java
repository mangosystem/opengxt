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
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryClipper;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

/**
 * Extracts input features that overlay the clip geometry.
 * 
 * @author Andrea Aime - GeoSolutions
 * @modifier Minpa Lee, MangoSystem
 * 
 * @reference org.geotools.process.vector.ClipProcess.java
 * 
 * @source $URL$
 */
public class ClipWithGeometryFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(ClipWithGeometryFeatureCollection.class);

    private Geometry clip;

    private SimpleFeatureType targetSchema;

    public ClipWithGeometryFeatureCollection(SimpleFeatureCollection delegate, Geometry clip) {
        super(delegate);

        this.clip = clip;
        this.targetSchema = buildTargetSchema(delegate.getSchema());
    }

    /**
     * When clipping lines and polygons can turn into multilines and multipolygons
     */
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

    @Override
    public SimpleFeatureIterator features() {
        return new ClipWithGeometryFeatureIterator(delegate.features(), clip, getSchema());
    }

    @Override
    public SimpleFeatureType getSchema() {
        return targetSchema;
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

    static class ClipWithGeometryFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private GeometryClipper clipper;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private Geometry clip;

        public ClipWithGeometryFeatureIterator(SimpleFeatureIterator delegate, Geometry clip,
                SimpleFeatureType schema) {
            this.delegate = delegate;

            // can we use the fast clipper?
            if (clip.getEnvelope().equals(clip)) {
                this.clipper = new GeometryClipper(clip.getEnvelopeInternal());
            } else {
                this.clip = clip;
            }

            builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                boolean clippedOut = false;

                // try building the clipped feature out of the original feature, if the
                // default geometry is clipped out, skip it
                SimpleFeature feature = delegate.next();
                GeometryDescriptor gds = feature.getFeatureType().getGeometryDescriptor();
                Object cliped = clipGeometry((Geometry) feature.getDefaultGeometry(), gds.getType()
                        .getBinding());
                if (cliped == null) {
                    clippedOut = true;
                }

                if (!clippedOut) {
                    // build the next feature
                    for (Object attribute : feature.getAttributes()) {
                        if (attribute instanceof Geometry) {
                            attribute = cliped;
                        }
                        builder.add(attribute);
                    }
                    next = builder.buildFeature(feature.getID());
                }
                builder.reset();
            }

            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature result = next;
            next = null;
            return result;
        }

        private Object clipGeometry(Geometry geom, Class<?> target) {
            // first off, clip
            Geometry clipped = null;
            if (clipper != null) {
                clipped = clipper.clip(geom, true);
            } else {
                if (geom.getEnvelopeInternal().intersects(clip.getEnvelopeInternal())) {
                    clipped = clip.intersection(geom);
                }
            }

            // empty intersection?
            if (clipped == null || clipped.getNumGeometries() == 0) {
                return null;
            }

            // map the result to the target output type, removing the spurious lower dimensional
            // elements that might result out of the intersection
            Geometry result;
            if (Point.class.isAssignableFrom(target) || MultiPoint.class.isAssignableFrom(target)
                    || GeometryCollection.class.equals(target)) {
                result = clipped;
            } else if (MultiLineString.class.isAssignableFrom(target)
                    || LineString.class.isAssignableFrom(target)) {
                final List<LineString> geoms = new ArrayList<LineString>();
                clipped.apply(new GeometryComponentFilter() {

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
                    LineString[] lsArray = (LineString[]) geoms
                            .toArray(new LineString[geoms.size()]);
                    result = geom.getFactory().createMultiLineString(lsArray);
                }
            } else if (MultiPolygon.class.isAssignableFrom(target)
                    || Polygon.class.isAssignableFrom(target)) {
                final List<Polygon> geoms = new ArrayList<Polygon>();
                clipped.apply(new GeometryComponentFilter() {

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
                    Polygon[] lsArray = (Polygon[]) geoms.toArray(new Polygon[geoms.size()]);
                    result = geom.getFactory().createMultiPolygon(lsArray);
                }
            } else {
                throw new RuntimeException("Unrecognized target type " + target.getCanonicalName());
            }

            return result;
        }
    }
}
