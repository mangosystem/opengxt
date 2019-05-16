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
import org.geotools.process.spatialstatistics.enumeration.PointLocationType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Vertices To Points SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class VerticesToPointsFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(VerticesToPointsFeatureCollection.class);

    private PointLocationType location;

    private SimpleFeatureType schema;

    public VerticesToPointsFeatureCollection(SimpleFeatureCollection delegate,
            PointLocationType location) {
        super(delegate);

        this.location = location;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Point.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new VerticesToPointsFeatureIterator(delegate.features(), getSchema(), location);
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
        switch (location) {
        case Start:
        case End:
        case Mid:
            return delegate.size();
        case BothEnds:
            return delegate.size() * 2;
        default:
            return DataUtilities.count(features());
        }
    }

    @Override
    public ReferencedEnvelope getBounds() {
        switch (location) {
        case All:
            return delegate.getBounds();
        default:
            return DataUtilities.bounds(features());
        }
    }

    static class VerticesToPointsFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private PointLocationType location;

        private int index = 0;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        private String typeName;

        public VerticesToPointsFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, PointLocationType location) {
            this.delegate = delegate;

            this.location = location;
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
                }

                // create feature
                Geometry geometry = (Geometry) origFeature.getDefaultGeometry();
                for (Object attribute : origFeature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        Coordinate midpoint = null;
                        Coordinate[] coords = geometry.getCoordinates();
                        switch (location) {
                        case All:
                            midpoint = coords[index];
                            index++;
                            if (index >= coords.length) {
                                index = 0;
                                origFeature = null;
                            }
                            break;
                        case Start:
                            midpoint = geometry.getCoordinate();
                            index = 0;
                            origFeature = null;
                            break;
                        case End:
                            midpoint = coords[coords.length - 1];
                            index = 0;
                            origFeature = null;
                            break;
                        case Mid:
                            Class<?> binding = geometry.getClass();
                            Geometry linearGeom = geometry;
                            if (Polygon.class.equals(binding)
                                    || MultiPolygon.class.equals(binding)) {
                                linearGeom = geometry.getBoundary();
                            }
                            LengthIndexedLine lil = new LengthIndexedLine(linearGeom);
                            midpoint = lil.extractPoint(linearGeom.getLength() / 2.0);
                            index = 0;
                            origFeature = null;
                            break;
                        case BothEnds:
                            if (index == 0) {
                                midpoint = geometry.getCoordinate();
                                index++;
                            } else {
                                midpoint = coords[coords.length - 1];
                                index = 0;
                                origFeature = null;
                            }
                            break;
                        }
                        attribute = geometry.getFactory().createPoint(midpoint);
                    }
                    builder.add(attribute);
                }

                nextFeature = builder.buildFeature(buildID(typeName, ++featureID));
                builder.reset();
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
    }
}
