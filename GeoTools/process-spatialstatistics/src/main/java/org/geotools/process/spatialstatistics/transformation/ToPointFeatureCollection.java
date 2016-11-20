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
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Feature To Point SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ToPointFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ToPointFeatureCollection.class);

    private boolean useInside;

    private SimpleShapeType shapeType;

    private SimpleFeatureType schema;

    public ToPointFeatureCollection(SimpleFeatureCollection delegate, boolean useInside) {
        super(delegate);

        this.useInside = useInside;
        this.shapeType = FeatureTypes.getSimpleShapeType(delegate.getSchema());

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Point.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ToPointFeatureIterator(delegate.features(), getSchema(), useInside, shapeType);
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
    public int size() {
        return delegate.size();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    static class ToPointFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private boolean useInside = false;

        private SimpleFeatureBuilder builder;

        private SimpleShapeType shapeType;

        public ToPointFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                boolean useInside, SimpleShapeType shapeType) {
            this.delegate = delegate;

            this.useInside = useInside;
            this.shapeType = shapeType;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();

            for (Object attribute : sourceFeature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    // centroid or interior point
                    Geometry geometry = (Geometry) attribute;
                    Point center = geometry.getCentroid();
                    if (useInside && shapeType == SimpleShapeType.POLYGON
                            && !geometry.contains(center)) {
                        center = geometry.getInteriorPoint();
                    }
                    attribute = center;
                }
                builder.add(attribute);
            }
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());
            builder.reset();

            return nextFeature;
        }
    }
}
