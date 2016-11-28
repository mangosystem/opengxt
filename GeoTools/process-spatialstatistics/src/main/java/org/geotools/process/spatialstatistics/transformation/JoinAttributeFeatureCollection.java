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

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.Join;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

/**
 * Join Attribute SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class JoinAttributeFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(JoinAttributeFeatureCollection.class);

    // TODO : GeoTools v.8.x use Join Query?

    private SimpleFeatureType schema;

    private String primaryKey;

    private SimpleFeatureCollection joinFeatures;

    private String foreignKey;

    private Join.Type joinType;

    private Hashtable<String, String> joinFields;

    public JoinAttributeFeatureCollection(SimpleFeatureCollection inputFeatures, String primaryKey,
            SimpleFeatureCollection joinFeatures, String foreignKey, Join.Type joinType) {
        super(inputFeatures);

        this.joinFields = new Hashtable<String, String>();
        this.joinType = joinType;
        this.primaryKey = FeatureTypes.validateProperty(inputFeatures.getSchema(), primaryKey);
        this.foreignKey = FeatureTypes.validateProperty(joinFeatures.getSchema(), foreignKey);
        this.joinFeatures = joinFeatures;
        this.schema = buildTargetSchema(inputFeatures.getSchema(), joinFeatures.getSchema());
    }

    private SimpleFeatureType buildTargetSchema(SimpleFeatureType originSchema,
            SimpleFeatureType destSchema) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setNamespaceURI(originSchema.getName().getNamespaceURI());
        builder.setName(originSchema.getTypeName());

        // 0. default geometry descriptor
        if (originSchema.getGeometryDescriptor() != null) {
            GeometryDescriptor descriptor = originSchema.getGeometryDescriptor();
            builder.setCRS(descriptor.getCoordinateReferenceSystem());
            builder.add(descriptor);
        } else {
            if (destSchema.getGeometryDescriptor() != null) {
                GeometryDescriptor descriptor = destSchema.getGeometryDescriptor();
                builder.setCRS(descriptor.getCoordinateReferenceSystem());
                builder.add(descriptor);
            }
        }

        // 1. first schema
        for (AttributeDescriptor ad : originSchema.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                continue; // skip geometry
            } else {
                builder.add(ad);
            }
        }

        // 2. second schema
        for (AttributeDescriptor ad : destSchema.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                continue; // skip geometry
            } else {
                String name = ad.getLocalName();
                if (originSchema.indexOf(name) == -1) {
                    builder.add(ad);
                    joinFields.put(ad.getLocalName(), name);
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
                        builder.length(length);
                    }

                    builder.minOccurs(ad.getMinOccurs());
                    builder.maxOccurs(ad.getMaxOccurs());
                    builder.nillable(ad.isNillable());
                    builder.add(name, binding);
                    joinFields.put(ad.getLocalName(), name);
                }
            }
        }

        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new AttributeJoinFeatureIterator(delegate.features(), getSchema(), primaryKey,
                joinFeatures, foreignKey, joinType, joinFields);
    }

    static class AttributeJoinFeatureIterator implements SimpleFeatureIterator {

        private SimpleFeatureIterator originIter;

        private String primaryKey;

        private SimpleFeatureCollection joinFeatures;

        private String foreignKey;

        private Join.Type joinType;

        private Hashtable<String, String> joinFields;

        private SimpleFeatureBuilder builder;

        private SimpleFeature nextFeature = null;

        private String typeName;

        private int counter = 1;

        public AttributeJoinFeatureIterator(SimpleFeatureIterator originIter,
                SimpleFeatureType schema, String primaryKey, SimpleFeatureCollection joinFeatures,
                String foreignKey, Join.Type joinType, Hashtable<String, String> joinFields) {
            this.originIter = originIter;
            this.primaryKey = primaryKey;
            this.joinFeatures = joinFeatures;
            this.foreignKey = foreignKey;
            this.joinType = joinType;
            this.joinFields = joinFields;
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
            this.counter = 1;
        }

        public void close() {
            originIter.close();
        }

        public boolean hasNext() {
            nextFeature = null;
            boolean hasNext = originIter.hasNext();
            if (!hasNext) {
                return false;
            }

            SimpleFeature origin = originIter.next();
            for (AttributeDescriptor ad : origin.getFeatureType().getAttributeDescriptors()) {
                Object value = origin.getAttribute(ad.getLocalName());
                builder.set(ad.getLocalName(), value);
            }

            boolean hasJoin = false;
            Object keyValue = origin.getAttribute(primaryKey);
            Filter filter = ff.equal(ff.property(foreignKey), ff.literal(keyValue), true);
            SimpleFeatureIterator destIter = joinFeatures.subCollection(filter).features();
            try {
                while (destIter.hasNext()) {
                    SimpleFeature dest = destIter.next();
                    for (Entry<String, String> entry : joinFields.entrySet()) {
                        Object value = dest.getAttribute(entry.getKey());
                        builder.set(entry.getValue(), value);
                    }
                    hasJoin = true;
                    nextFeature = builder.buildFeature(buildID(typeName, counter++));
                    builder.reset();
                    break; // one to one
                }
            } finally {
                destIter.close();
            }

            if (joinType == Join.Type.INNER && !hasJoin) {
                return hasNext();
            } else if (joinType == Join.Type.OUTER && !hasJoin) {
                nextFeature = builder.buildFeature(buildID(typeName, counter++));
                builder.reset();
            }

            return true;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            return nextFeature;
        }
    }
}