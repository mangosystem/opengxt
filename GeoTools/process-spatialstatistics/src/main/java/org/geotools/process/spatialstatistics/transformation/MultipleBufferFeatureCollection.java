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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a new features of buffer features using a set of buffer distances.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MultipleBufferFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(MultipleBufferFeatureCollection.class);

    static final String bufferField = "distance";

    private double[] distances;

    private Boolean outsideOnly = Boolean.TRUE;

    private SimpleFeatureType schema;

    public MultipleBufferFeatureCollection(SimpleFeatureCollection delegate, double[] distances,
            Boolean outsideOnly) {
        super(delegate);

        Arrays.sort(distances);
        this.distances = distances;
        this.outsideOnly = outsideOnly;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
        this.schema = FeatureTypes.add(schema, bufferField, Double.class, 19);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new BufferedFeatureIterator(delegate, getSchema(), distances, outsideOnly);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        ReferencedEnvelope bounds = delegate.getBounds();
        bounds.expandBy(distances[distances.length - 1]);
        return bounds;
    }

    @Override
    public int size() {
        return delegate.size() * distances.length;
    }

    /**
     * Buffers each feature as we scroll over the collection
     */
    static class BufferedFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private double[] distances;

        private Boolean outsideOnly = Boolean.TRUE;

        private int bufferIndex = 0;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature nextFeature = null;

        private SimpleFeature origFeature = null;

        public BufferedFeatureIterator(SimpleFeatureCollection delegate, SimpleFeatureType schema,
                double[] distances, Boolean outsideOnly) {
            this.delegate = delegate.features();

            this.bufferIndex = 0;
            this.distances = distances;
            this.outsideOnly = outsideOnly;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while ((nextFeature == null && delegate.hasNext())
                    || (nextFeature == null && !delegate.hasNext() && bufferIndex > 0)) {
                if (bufferIndex == 0) {
                    origFeature = delegate.next();
                }

                // buffer geometry
                Geometry orig = (Geometry) origFeature.getDefaultGeometry();
                Geometry buff = orig.buffer(distances[bufferIndex], 24);
                if (outsideOnly && bufferIndex > 0) {
                    buff = buff.difference(orig.buffer(distances[bufferIndex - 1], 24));
                }

                // create feature
                nextFeature = builder.buildFeature(Integer.toString(++featureID));
                transferAttribute(origFeature, nextFeature);
                nextFeature.setDefaultGeometry(buff);
                nextFeature.setAttribute(bufferField, distances[bufferIndex]);

                builder.reset();
                bufferIndex++;

                if (bufferIndex >= distances.length) {
                    bufferIndex = 0;
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
    }
}
