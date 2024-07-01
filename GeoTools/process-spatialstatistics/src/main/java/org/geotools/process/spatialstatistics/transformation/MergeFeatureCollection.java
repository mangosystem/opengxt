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

import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

/**
 * Merge SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MergeFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(MergeFeatureCollection.class);

    private SimpleFeatureCollection features;

    private SimpleFeatureType schema;

    public MergeFeatureCollection(SimpleFeatureCollection delegate, SimpleFeatureCollection features) {
        super(delegate);

        // check coordinate reference system
        CoordinateReferenceSystem crsT = delegate.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = features.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            this.features = new ReprojectFeatureCollection(features, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        } else {
            this.features = features;
        }

        this.schema = buildTargetSchema();
    }

    private SimpleFeatureType buildTargetSchema() {
        // Create schema containing the attributes from both the feature collections
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor descriptor : delegate.getSchema().getAttributeDescriptors()) {
            if (sameNames(features.getSchema(), descriptor)
                    && !sameTypes(features.getSchema(), descriptor)) {
                AttributeTypeBuilder builder = new AttributeTypeBuilder();
                builder.setName(descriptor.getLocalName());
                builder.setNillable(descriptor.isNillable());
                builder.setBinding(String.class); // to string
                builder.setMinOccurs(descriptor.getMinOccurs());
                builder.setMaxOccurs(descriptor.getMaxOccurs());
                builder.setDefaultValue(descriptor.getDefaultValue());
                builder.setCRS(delegate.getSchema().getCoordinateReferenceSystem());
                AttributeDescriptor attributeDescriptor = builder.buildDescriptor(
                        descriptor.getName(), builder.buildType());
                tb.add(attributeDescriptor);
            } else {
                tb.add(descriptor);
            }
        }

        for (AttributeDescriptor descriptor : features.getSchema().getAttributeDescriptors()) {
            if (!sameNames(delegate.getSchema(), descriptor)
                    && !sameTypes(delegate.getSchema(), descriptor)) {
                tb.add(descriptor);
            }
        }

        tb.setCRS(delegate.getSchema().getCoordinateReferenceSystem());
        tb.setNamespaceURI(delegate.getSchema().getName().getNamespaceURI());
        tb.setName(delegate.getSchema().getName());
        return tb.buildFeatureType();
    }

    @Override
    public SimpleFeatureIterator features() {
        return new MergeFeatureIterator(delegate, features, getSchema());
    }

    @Override
    public SimpleFeatureType getSchema() {
        return this.schema;
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
        return delegate.size() + features.size();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        ReferencedEnvelope bounds = delegate.getBounds();
        if (bounds == null) {
            bounds = features.getBounds();
        } else {
            if (features.getBounds() != null) {
                bounds.include(features.getBounds());
            }
        }
        return bounds;
    }

    private boolean sameNames(SimpleFeatureType schema, AttributeDescriptor f) {
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor.getName().equals(f.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean sameTypes(SimpleFeatureType schema, AttributeDescriptor f) {
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor.getType().equals(f.getType())) {
                return true;
            }
        }
        return false;
    }

    static class MergeFeatureIterator implements SimpleFeatureIterator {

        private SimpleFeatureIterator firstDelegate;

        private SimpleFeatureIterator secondDelegate;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private String typeName;

        private int featureID = 0;

        public MergeFeatureIterator(SimpleFeatureCollection firstCollection,
                SimpleFeatureCollection secondCollection, SimpleFeatureType schema) {
            this.firstDelegate = firstCollection.features();
            this.secondDelegate = secondCollection.features();
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            firstDelegate.close();
            secondDelegate.close();
        }

        public boolean hasNext() {
            while (next == null && firstDelegate.hasNext()) {
                SimpleFeature f = firstDelegate.next();
                for (PropertyDescriptor property : builder.getFeatureType().getDescriptors()) {
                    builder.set(property.getName(), f.getAttribute(property.getName()));
                }
                next = builder.buildFeature(buildID(typeName, featureID));
                builder.reset();
                featureID++;
            }

            while (next == null && secondDelegate.hasNext() && !firstDelegate.hasNext()) {
                SimpleFeature f = secondDelegate.next();
                for (PropertyDescriptor property : builder.getFeatureType().getDescriptors()) {
                    builder.set(property.getName(), f.getAttribute(property.getName()));
                }
                next = builder.buildFeature(buildID(typeName, featureID));
                builder.reset();
                featureID++;
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

    }
}