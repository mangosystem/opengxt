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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Transform SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ReprojectFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ReprojectFeatureCollection.class);

    private SimpleFeatureType schema;

    final GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();

    public ReprojectFeatureCollection(SimpleFeatureCollection delegate,
            CoordinateReferenceSystem forcedCRS, CoordinateReferenceSystem targetCRS,
            boolean lenient) {
        super(delegate);

        SimpleFeatureType schema = delegate.getSchema();
        if (forcedCRS == null) {
            forcedCRS = schema.getCoordinateReferenceSystem();
            if (forcedCRS == null) {
                throw new NullPointerException("forcedCRS CoordinateReferenceSystem");
            }
        }

        if (targetCRS == null) {
            throw new NullPointerException("targetCRS CoordinateReferenceSystem");
        }

        this.transformer.setMathTransform(transform(forcedCRS, targetCRS, lenient));
        this.transformer.setCoordinateReferenceSystem(targetCRS);

        this.schema = FeatureTypes.build(schema, schema.getTypeName(), targetCRS);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new TransformFeatureIterator(delegate.features(), getSchema(), transformer);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    private MathTransform transform(CoordinateReferenceSystem source,
            CoordinateReferenceSystem target, boolean lenient) {
        try {
            return CRS.findMathTransform(source, target, lenient);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Could not create math transform");
        }
    }

    static class TransformFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private GeometryCoordinateSequenceTransformer transformer;

        private SimpleFeatureBuilder builder;

        public TransformFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                GeometryCoordinateSequenceTransformer transformer) {
            this.delegate = delegate;
            this.transformer = transformer;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            for (Object attribute : feature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    try {
                        attribute = transformer.transform((Geometry) attribute);
                    } catch (TransformException e) {
                        String msg = "Error occured transforming " + attribute.toString();
                        LOGGER.log(Level.WARNING, msg);
                    }
                }
                builder.add(attribute);
            }
            return builder.buildFeature(feature.getID());
        }
    }
}
