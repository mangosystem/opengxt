/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.VariableBuffer;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import si.uom.SI;

/**
 * Creates a buffer polygon with a varying buffer distance at each vertex along a line.
 * <p>
 * Supported only on JTS 1.17.0 and later
 * 
 * @reference https://github.com/locationtech/jts/blob/master/modules/core/src/main/java/org/locationtech/jts/operation/buffer/VariableBuffer.java
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class VariableBufferFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(VariableBufferFeatureCollection.class);

    private Expression startDistance;

    private Expression endDistance;

    private DistanceUnit distanceUnit = DistanceUnit.Default;

    private SimpleFeatureType schema;

    public VariableBufferFeatureCollection(SimpleFeatureCollection lineFeatures,
            double startDistance, double endDistance) {
        this(lineFeatures, startDistance, endDistance, DistanceUnit.Default);
    }

    public VariableBufferFeatureCollection(SimpleFeatureCollection lineFeatures,
            double startDistance, double endDistance, DistanceUnit distanceUnit) {
        this(lineFeatures, ff.literal(startDistance), ff.literal(endDistance), distanceUnit);
    }

    public VariableBufferFeatureCollection(SimpleFeatureCollection lineFeatures,
            Expression startDistance, Expression endDistance) {
        this(lineFeatures, startDistance, endDistance, DistanceUnit.Default);
    }

    public VariableBufferFeatureCollection(SimpleFeatureCollection lineFeatures,
            Expression startDistance, Expression endDistance, DistanceUnit distanceUnit) {
        super(lineFeatures);

        this.startDistance = startDistance;
        this.endDistance = endDistance;
        this.distanceUnit = distanceUnit;

        String typeName = lineFeatures.getSchema().getTypeName();
        this.schema = FeatureTypes.build(lineFeatures.getSchema(), typeName, Polygon.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new VariableBufferFeatureIterator(delegate.features(), getSchema(), startDistance,
                endDistance, distanceUnit);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class VariableBufferFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private Expression startDistance;

        private Expression endDistance;

        private DistanceUnit distanceUnit = DistanceUnit.Default;

        private int count = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private Unit<Length> targetUnit = SI.METRE;

        private boolean isGeographicCRS = false;

        private GeometryCoordinateSequenceTransformer transformer;

        private String typeName;

        public VariableBufferFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression startDistance, Expression endDistance,
                DistanceUnit distanceUnit) {
            this.delegate = delegate;

            this.startDistance = startDistance;
            this.endDistance = endDistance;
            this.distanceUnit = distanceUnit;

            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            if (distanceUnit != DistanceUnit.Default) {
                this.targetUnit = UnitConverter.getLengthUnit(crs);
            }

            this.isGeographicCRS = UnitConverter.isGeographicCRS(crs);
            if (isGeographicCRS) {
                this.transformer = new GeometryCoordinateSequenceTransformer();
            }

            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature source = delegate.next();

                Double start = startDistance.evaluate(source, Double.class);
                Double end = endDistance.evaluate(source, Double.class);

                if (start != null && end != null) {
                    start = Math.max(start, 0d);
                    end = Math.max(end, 0d);

                    Geometry geometry = (Geometry) source.getDefaultGeometry();
                    Geometry buffered = buffer(geometry, start, end);

                    if (buffered != null) {
                        next = builder.buildFeature(buildID(typeName, ++count));

                        // transfer attributes
                        transferAttribute(source, next);

                        next.setDefaultGeometry(buffered);

                        builder.reset();
                    }
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

        private Geometry buffer(Geometry source, double startDistance, double endDistance) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            if (startDistance == 0d && endDistance == 0d) {
                return null;
            }

            double start = startDistance;
            double end = endDistance;
            if (distanceUnit != DistanceUnit.Default) {
                if (isGeographicCRS) {
                    start = UnitConverter.convertDistance(startDistance, distanceUnit, SI.METRE);
                    end = UnitConverter.convertDistance(endDistance, distanceUnit, SI.METRE);
                } else {
                    start = UnitConverter.convertDistance(startDistance, distanceUnit, targetUnit);
                    end = UnitConverter.convertDistance(endDistance, distanceUnit, targetUnit);
                }
            }

            List<Geometry> parts = new ArrayList<>();
            for (int index = 0; index < source.getNumGeometries(); index++) {
                Geometry lineString = source.getGeometryN(index); // Only LineSTring
                Geometry buffered = null;
                try {
                    if (isGeographicCRS) {
                        buffered = bufferAutoUTM(lineString, start, end);
                    } else {
                        buffered = variableBuffer(lineString, start, end);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }

                if (buffered == null || buffered.isEmpty()) {
                    continue;
                }

                parts.add(buffered);
            }

            return parts.size() > 0 ? CascadedPolygonUnion.union(parts) : null;
        }

        private Geometry variableBuffer(Geometry source, double startDistance, double endDistance) {
            Geometry buffered = null;
            try {
                buffered = VariableBuffer.buffer(source, startDistance, endDistance);
            } catch (Exception e) {
                if (startDistance == 0d) {
                    startDistance = 1.0E-8;
                    buffered = VariableBuffer.buffer(source, startDistance, endDistance);
                }
            }
            return buffered;
        }

        private Geometry bufferAutoUTM(Geometry source, double startDistance, double endDistance)
                throws Exception {
            Coordinate center = source.getEnvelopeInternal().centre();

            // WGS 84 / Auto UTM: "AUTO:42001, lon, lat"
            String code = String.format("AUTO:42001, %s, %s", center.x, center.y);
            CoordinateReferenceSystem autoCRS = CRS.decode(code);

            MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, autoCRS);

            // WGS84 --> AUTO
            transformer.setMathTransform(transform);
            Geometry projected = transformer.transform(source);

            // Buffer
            Geometry buffered = null;
            buffered = variableBuffer(projected, startDistance, endDistance);
            if (buffered == null || buffered.isEmpty()) {
                return null;
            }

            // AUTO --> WGS84
            transformer.setMathTransform(transform.inverse());

            return transformer.transform(buffered);
        }
    }
}