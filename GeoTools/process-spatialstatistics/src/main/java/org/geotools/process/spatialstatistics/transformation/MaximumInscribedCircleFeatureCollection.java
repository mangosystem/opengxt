/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2022, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Feature To Maximum Inscribed Circle SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MaximumInscribedCircleFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(MaximumInscribedCircleFeatureCollection.class);

    private boolean singlePart;

    private SimpleFeatureType schema;

    public MaximumInscribedCircleFeatureCollection(SimpleFeatureCollection delegate,
            boolean singlePart) {
        super(delegate);

        Class<?> binding = delegate.getSchema().getGeometryDescriptor().getType().getBinding();
        if (!Polygon.class.equals(binding) && !MultiPolygon.class.equals(binding)) {
            throw new ProcessException("The feature type must be Polygon or MultiPolygon!");
        }

        this.singlePart = singlePart;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new MaximumInscribedCircleFeatureIterator(delegate.features(), getSchema(),
                singlePart);
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

    static class MaximumInscribedCircleFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private boolean singlePart = false;

        private SimpleFeature next;

        private SimpleFeatureBuilder builder;

        public MaximumInscribedCircleFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, boolean singlePart) {
            this.delegate = delegate;

            this.singlePart = singlePart;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature source = delegate.next();
                Geometry geometry = (Geometry) source.getDefaultGeometry();

                Geometry circle = null;
                if (!singlePart && geometry.getNumGeometries() > 1) {
                    List<Geometry> geomList = new ArrayList<Geometry>();
                    for (int index = 0; index < geometry.getNumGeometries(); index++) {
                        Geometry part = geometry.getGeometryN(index);
                        geomList.add(maximumInscribedCircle(part));
                    }
                    circle = geometry.getFactory().buildGeometry(geomList);
                } else {
                    circle = maximumInscribedCircle(geometry);
                }

                if (circle != null && !circle.isEmpty()) {
                    next = builder.buildFeature(source.getID());

                    // transfer attributes
                    transferAttribute(source, next);

                    next.setDefaultGeometry(circle);

                    builder.reset();
                }
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

        private Geometry maximumInscribedCircle(Geometry source) {
            MaximumInscribedCircle mic = new MaximumInscribedCircle(source, 1.0);
            Geometry radiusLine = mic.getRadiusLine();
            if (radiusLine == null || radiusLine.isEmpty() || radiusLine.getLength() == 0) {
                return null;
            } else {
                Geometry center = mic.getCenter();
                return center.buffer(radiusLine.getLength(), 24);
            }
        }
    }
}