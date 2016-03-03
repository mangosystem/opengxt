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
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Intersect SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IntersectFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(IntersectFeatureCollection.class);

    private SimpleFeatureCollection overlays;

    private SimpleFeatureType targetSchema;

    private Hashtable<String, String> fieldMap;

    public IntersectFeatureCollection(SimpleFeatureCollection delegate,
            SimpleFeatureCollection overlays) {
        super(delegate);

        this.fieldMap = new Hashtable<String, String>();
        this.overlays = overlays;
        this.targetSchema = buildTargetSchema(delegate.getSchema(), overlays.getSchema());
    }

    private SimpleFeatureType buildTargetSchema(SimpleFeatureType originSchema,
            SimpleFeatureType destSchema) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setNamespaceURI(originSchema.getName().getNamespaceURI());
        tb.setName(originSchema.getTypeName());

        // 1. first schema
        for (AttributeDescriptor ad : originSchema.getAttributeDescriptors()) {
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

        // 2. second schema
        for (AttributeDescriptor ad : destSchema.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                continue; // skip geometry
            } else {
                String name = ad.getLocalName();
                if (originSchema.indexOf(name) == -1) {
                    tb.add(ad);
                    fieldMap.put(ad.getLocalName(), name);
                } else {
                    // get unique field name
                    for (int index = 1; index < Integer.MAX_VALUE; index++) {
                        name = ad.getLocalName() + "_" + index;
                        if (originSchema.indexOf(name) == -1) {
                            break;
                        }
                    }

                    // build AttributeDescriptor
                    Class<?> binding = ad.getType().getBinding();
                    if (CharSequence.class.isAssignableFrom(binding)) {
                        int length = FeatureTypes.getAttributeLength(ad);
                        if (length == 0) {
                            length = 254; // string default length
                        }
                        tb.length(length);
                    }

                    tb.minOccurs(ad.getMinOccurs());
                    tb.maxOccurs(ad.getMaxOccurs());
                    tb.nillable(ad.isNillable());
                    tb.add(name, binding);
                    fieldMap.put(ad.getLocalName(), name);
                }
            }
        }

        return tb.buildFeatureType();
    }

    @Override
    public SimpleFeatureType getSchema() {
        return targetSchema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new IntersectFeatureIterator(delegate.features(), getSchema(), overlays, fieldMap);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    public int size() {
        return DataUtilities.count(features());
    }

    static class IntersectFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private SimpleFeatureCollection overlays;

        private String geomField;

        private SimpleFeatureBuilder builder;

        private Hashtable<String, String> fieldMap;

        private SimpleFeature next;

        private SimpleFeature currentFeature;

        private Class<?> target;

        private int counter = 1;

        private List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        public IntersectFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                SimpleFeatureCollection overlays, Hashtable<String, String> fieldMap) {
            this.delegate = delegate;
            this.overlays = overlays;
            this.geomField = overlays.getSchema().getGeometryDescriptor().getLocalName();
            this.builder = new SimpleFeatureBuilder(schema);
            this.target = schema.getGeometryDescriptor().getType().getBinding();
            this.fieldMap = fieldMap;
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((next == null && delegate.hasNext())
                    || (next == null && !delegate.hasNext() && features.size() > 0)) {
                if (features.size() == 0) {
                    // query intersected features
                    currentFeature = delegate.next();
                    Geometry currentGeom = (Geometry) currentFeature.getDefaultGeometry();
                    if (currentGeom == null || currentGeom.isEmpty()) {
                        continue;
                    }

                    Filter filter = ff.intersects(ff.property(geomField), ff.literal(currentGeom));
                    SimpleFeatureIterator difIter = overlays.subCollection(filter).features();
                    try {
                        while (difIter.hasNext()) {
                            features.add(difIter.next());
                        }
                    } finally {
                        difIter.close();
                    }
                }

                if (features.size() == 0) {
                    continue;
                }

                Geometry currentGeom = (Geometry) currentFeature.getDefaultGeometry();
                SimpleFeature overlayFeature = features.get(0);
                Geometry overlayGeom = (Geometry) overlayFeature.getDefaultGeometry();
                Geometry result = intersect(currentGeom, overlayGeom, target);
                if (result == null || result.isEmpty()) {
                    features.remove(0);
                    continue;
                }

                // input feature
                for (Object attribute : currentFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        builder.add(result);
                    } else {
                        builder.add(attribute);
                    }
                }

                // overlay feature
                for (Entry<String, String> entry : fieldMap.entrySet()) {
                    Object value = overlayFeature.getAttribute(entry.getKey());
                    builder.set(entry.getValue(), value);
                }

                next = builder.buildFeature(Integer.toString(counter++));
                builder.reset();
                features.remove(0);
                break;
            }

            return next != null;
        }

        private Geometry intersect(Geometry geom, Geometry overlay, Class<?> target) {
            Geometry intersection = geom.intersection(overlay);

            // empty intersection?
            if (intersection == null || intersection.getNumGeometries() == 0) {
                return null;
            }

            // map the result to the target output type, removing the spurious lower dimensional
            // elements that might result out of the intersection
            Geometry result;
            if (Point.class.isAssignableFrom(target) || MultiPoint.class.isAssignableFrom(target)
                    || GeometryCollection.class.equals(target)) {
                result = intersection;
            } else if (MultiLineString.class.isAssignableFrom(target)
                    || LineString.class.isAssignableFrom(target)) {
                final List<LineString> geoms = new ArrayList<LineString>();
                intersection.apply(new GeometryComponentFilter() {

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
                intersection.apply(new GeometryComponentFilter() {

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