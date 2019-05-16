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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Splits line features based on their vertices.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SplitLineFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(SplitLineFeatureCollection.class);

    private SimpleFeatureType schema;

    public SplitLineFeatureCollection(SimpleFeatureCollection delegate) {
        super(delegate);

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, LineString.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SplitLineFeatureIterator(delegate.features(), getSchema());
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
        return DataUtilities.count(features());
    }

    static class SplitLineFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int index = 0;

        private List<Geometry> segments;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        private String typeName;

        public SplitLineFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema) {
            this.delegate = delegate;

            this.index = 0;
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((nextFeature == null && delegate.hasNext())
                    || (nextFeature == null && !delegate.hasNext() && index > 0)) {
                if (index == 0) {
                    origFeature = delegate.next();
                    Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                    segments = splitLines(geometry);
                }

                // create feature
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = segments.get(index);
                    }
                    builder.add(attribute);
                }
                nextFeature = builder.buildFeature(buildID(typeName, ++featureID));
                builder.reset();
                index++;

                if (index >= segments.size()) {
                    index = 0;
                    origFeature = null;
                }
            }
            return nextFeature != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = nextFeature;
            nextFeature = null;
            return result;
        }

        private List<Geometry> splitLines(Geometry geometry) {
            List<Geometry> segments = new ArrayList<Geometry>();

            Geometry lineString = geometry;
            Class<?> binding = geometry.getClass();
            if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
                lineString = geometry.getBoundary();
            }

            for (int index = 0; index < lineString.getNumGeometries(); index++) {
                Geometry part = lineString.getGeometryN(index);
                Coordinate[] coords = part.getCoordinates();
                for (int i = 0, j = coords.length - 1; i < j; i++) {
                    LineSegment seg = new LineSegment(coords[i], coords[i + 1]);
                    Geometry splits = seg.toGeometry(geometry.getFactory());
                    splits.setUserData(geometry.getUserData());
                    segments.add(splits);
                }
            }

            return segments;
        }
    }
}
