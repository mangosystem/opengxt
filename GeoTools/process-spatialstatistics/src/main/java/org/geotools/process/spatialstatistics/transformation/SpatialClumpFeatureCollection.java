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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

/**
 * Creates a spatial clump map using point features and radius expression..
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialClumpFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(SpatialClumpFeatureCollection.class);

    private SimpleFeatureType schema;

    private Geometry multiPart = null;

    public SpatialClumpFeatureCollection(SimpleFeatureCollection delegate, Expression radius,
            int quadrantSegments) {
        this(delegate, radius, DistanceUnit.Default, quadrantSegments);
    }

    public SpatialClumpFeatureCollection(SimpleFeatureCollection delegate, Expression radius,
            DistanceUnit radiusUnit, int quadrantSegments) {
        super(delegate);

        String typeName = delegate.getSchema().getTypeName();
        CoordinateReferenceSystem crs = delegate.getSchema().getCoordinateReferenceSystem();
        this.schema = FeatureTypes.getDefaultType(typeName, Polygon.class, crs);
        this.schema = FeatureTypes.add(schema, "uid", Integer.class, 19);

        if (quadrantSegments <= 0) {
            quadrantSegments = 8; // default
        }

        this.buildGeometries(delegate, radius, radiusUnit, quadrantSegments);
    }

    private void buildGeometries(SimpleFeatureCollection features, Expression radius,
            DistanceUnit radiusUnit, int quadrantSegments) {
        SimpleFeatureCollection buffered = new BufferExpressionFeatureCollection(features, radius,
                radiusUnit, quadrantSegments);

        List<Geometry> geometries = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = buffered.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometries.add(geometry);
            }
        } finally {
            featureIter.close();
        }

        if (geometries.size() > 0) {
            CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
            this.multiPart = unionOp.union();
        } else {
            this.multiPart = null;
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SpatialClumpFeatureIterator(multiPart, getSchema());
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
        if (multiPart == null) {
            return 0;
        }
        return multiPart.getNumGeometries();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        if (multiPart == null) {
            return delegate.getBounds();
        } else {
            return new ReferencedEnvelope(multiPart.getEnvelopeInternal(),
                    schema.getCoordinateReferenceSystem());
        }
    }

    static class SpatialClumpFeatureIterator implements SimpleFeatureIterator {
        private Geometry multiPart;

        private int count = 0;

        private int index = 0;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private String typeName;

        public SpatialClumpFeatureIterator(Geometry multiPart, SimpleFeatureType schema) {
            this.multiPart = multiPart;
            this.count = multiPart == null ? 0 : multiPart.getNumGeometries();
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            // nothing to do
        }

        public boolean hasNext() {
            return count > index;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature next = builder.buildFeature(buildID(typeName, ++featureID));
            next.setDefaultGeometry(multiPart.getGeometryN(index++));
            next.setAttribute("uid", Integer.valueOf(index));

            return next;
        }
    }
}