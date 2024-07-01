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

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import si.uom.SI;

/**
 * Creates wedge shaped buffers on point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WedgeBufferFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(WedgeBufferFeatureCollection.class);

    private Expression azimuth;

    private Expression wedgeAngle;

    private Expression innerRadius;

    private Expression outerRadius;

    private DistanceUnit distanceUnit = DistanceUnit.Default;

    private SimpleFeatureType schema;

    public WedgeBufferFeatureCollection(SimpleFeatureCollection delegate, String azimuthField,
            String wedgeAngleField, String innerRadiusField, String outerRadiusField) {
        this(delegate, azimuthField, wedgeAngleField, innerRadiusField, outerRadiusField,
                DistanceUnit.Default);
    }

    public WedgeBufferFeatureCollection(SimpleFeatureCollection delegate, String azimuthField,
            String wedgeAngleField, String innerRadiusField, String outerRadiusField,
            DistanceUnit distanceUnit) {
        this(delegate, ff.literal(azimuthField), ff.literal(wedgeAngleField),
                ff.literal(innerRadiusField), ff.literal(outerRadiusField), distanceUnit);
    }

    public WedgeBufferFeatureCollection(SimpleFeatureCollection delegate, Expression azimuth,
            Expression wedgeAngle, Expression innerRadius, Expression outerRadius) {
        this(delegate, azimuth, wedgeAngle, innerRadius, outerRadius, DistanceUnit.Default);
    }

    public WedgeBufferFeatureCollection(SimpleFeatureCollection delegate, Expression azimuth,
            Expression wedgeAngle, Expression innerRadius, Expression outerRadius,
            DistanceUnit distanceUnit) {
        super(delegate);

        this.azimuth = azimuth;
        this.wedgeAngle = wedgeAngle;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.distanceUnit = distanceUnit;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new WedgeBufferFeatureIterator(delegate.features(), getSchema(), azimuth, wedgeAngle,
                innerRadius, outerRadius, distanceUnit);
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

    static class WedgeBufferFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        static final int SEG = 24;

        private Expression azimuthExp;

        private Expression wedgeAngleExp;

        private Expression outerRadiusExp;

        private Expression innerRadiusExp;

        private DistanceUnit distanceUnit = DistanceUnit.Default;

        private Unit<Length> targetUnit = SI.METRE;

        private int count = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private String typeName;

        private boolean isGeographicCRS = false;

        private GeodeticBuilder geodetic;

        public WedgeBufferFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                Expression azimuth, Expression wedgeAngle, Expression innerRadius,
                Expression outerRadius, DistanceUnit distanceUnit) {
            this.delegate = delegate;

            this.azimuthExp = azimuth;
            this.wedgeAngleExp = wedgeAngle;
            this.innerRadiusExp = innerRadius;
            this.outerRadiusExp = outerRadius;
            this.distanceUnit = distanceUnit;
            this.targetUnit = UnitConverter.getLengthUnit(schema.getCoordinateReferenceSystem());

            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();

            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            this.isGeographicCRS = UnitConverter.isGeographicCRS(crs);
            if (isGeographicCRS) {
                geodetic = new GeodeticBuilder(crs);
                geodetic.setQuadrantSegments(SEG);
            }
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature source = delegate.next();
                Double azimuth = azimuthExp.evaluate(source, Double.class);
                Double wedgeAngle = wedgeAngleExp.evaluate(source, Double.class);
                if (azimuth == null || wedgeAngle == null || wedgeAngle <= 0) {
                    continue;
                }

                Double innerRadius = Double.valueOf(0d);
                if (innerRadiusExp != null) {
                    innerRadius = innerRadiusExp.evaluate(source, Double.class);
                    if (innerRadius == null || innerRadius < 0) {
                        innerRadius = Double.valueOf(0d);
                    }
                }

                Double outerRadius = Double.valueOf(0d);
                if (outerRadiusExp != null) {
                    outerRadius = outerRadiusExp.evaluate(source, Double.class);
                    if (outerRadius == null || outerRadius < 0) {
                        outerRadius = Double.valueOf(0d);
                    }
                }

                if (innerRadius == 0 && outerRadius == 0) {
                    continue;
                }

                double inner = 0d;
                double outer = 0d;

                if (isGeographicCRS) {
                    inner = UnitConverter.convertDistance(innerRadius, distanceUnit, SI.METRE);
                    outer = UnitConverter.convertDistance(outerRadius, distanceUnit, SI.METRE);
                } else {
                    inner = UnitConverter.convertDistance(innerRadius, distanceUnit, targetUnit);
                    outer = UnitConverter.convertDistance(outerRadius, distanceUnit, targetUnit);
                }

                Geometry geometry = (Geometry) source.getDefaultGeometry();
                Point centroid = geometry.getCentroid();
                Geometry buffered = null;

                try {
                    buffered = this.bufferWedge(centroid, azimuth, wedgeAngle, inner, outer);
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.INFO, e.getMessage());
                }

                if (buffered == null || buffered.isEmpty()) {
                    continue;
                }

                for (Object attribute : source.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = buffered;
                    }
                    builder.add(attribute);
                }

                next = builder.buildFeature(buildID(typeName, ++count));
                builder.reset();
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

        private Geometry bufferWedge(Point centroid, double azimuth, double wedgeAngle,
                double innerRadius, double outerRadius) {
            double minRadius = Math.min(outerRadius, innerRadius);
            double maxRadius = Math.max(outerRadius, innerRadius);

            if (wedgeAngle >= 360) {
                Geometry buffered = null;
                if (isGeographicCRS) {
                    try {
                        buffered = geodetic.buffer(centroid, maxRadius);
                        if (minRadius > 0 && minRadius != maxRadius) {
                            buffered = buffered.difference(geodetic.buffer(centroid, minRadius));
                        }
                    } catch (FactoryException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    } catch (TransformException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }
                } else {
                    buffered = centroid.buffer(maxRadius, SEG);
                    if (minRadius > 0 && minRadius != maxRadius) {
                        buffered = buffered.difference(centroid.buffer(minRadius, SEG));
                    }
                }
                return buffered;
            }
            return createWedgeBuffer(centroid.getCoordinate(), azimuth, wedgeAngle, minRadius,
                    maxRadius);
        }

        private Geometry createWedgeBuffer(Coordinate centroid, double azimuth, double wedgeAngle,
                double minRadius, double maxRadius) {
            CoordinateList coords = new CoordinateList();

            // make azimuth 0 north and positive clockwise (compass direction)
            double degree = 90 - azimuth;
            double fromDegree = degree - wedgeAngle * 0.5;
            double toDegree = degree + wedgeAngle * 0.5;
            double increment = Math.abs(toDegree - fromDegree) / SEG;

            if (minRadius > 0) {
                for (int i = SEG; i >= 0; i--) {
                    Coordinate coordinate = null;
                    double deg = fromDegree + (i * increment);
                    if (isGeographicCRS) {
                        coordinate = geodetic.getDestination(centroid, 90 - deg, minRadius);
                    } else {
                        double radian = Math.toRadians(deg);
                        coordinate = createPoint(centroid, radian, minRadius);
                    }
                    coords.add(coordinate, false);
                }
            } else {
                coords.add(centroid, false);
            }

            // outer
            for (int i = 0; i <= SEG; i++) {
                Coordinate coordinate = null;
                double deg = fromDegree + (i * increment);
                if (isGeographicCRS) {
                    coordinate = geodetic.getDestination(centroid, 90 - deg, maxRadius);
                } else {
                    double radian = Math.toRadians(deg);
                    coordinate = createPoint(centroid, radian, maxRadius);
                }
                coords.add(coordinate, false);
            }

            // close ring
            coords.add(coords.getCoordinate(0), true);

            return new GeometryFactory().createPolygon(coords.toCoordinateArray());
        }

        private Coordinate createPoint(Coordinate centroid, double radian, double radius) {
            double dx = Math.cos(radian) * radius;
            double dy = Math.sin(radian) * radius;
            return new Coordinate(centroid.x + dx, centroid.y + dy);
        }
    }
}