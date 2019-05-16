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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.util.Assert;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Repair Geometry SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RepairGeometryFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(RepairGeometryFeatureCollection.class);

    public RepairGeometryFeatureCollection(SimpleFeatureCollection delegate) {
        super(delegate);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new RepairGeometryFeatureIterator(delegate.features(), getSchema());
    }

    @Override
    public int size() {
        return DataUtilities.count(features());
    }

    static class RepairGeometryFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private int featureID = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private String typeName;

        public RepairGeometryFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema) {
            this.delegate = delegate;

            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                // repair geometry
                SimpleFeature feature = delegate.next();
                Geometry repaired = validate((Geometry) feature.getDefaultGeometry());
                if (repaired == null) {
                    continue;
                }

                // build the next feature
                for (Object attribute : feature.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = repaired;
                    }
                    builder.add(attribute);
                }
                next = builder.buildFeature(buildID(typeName, ++featureID));
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

        private Geometry validate(Geometry source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            if (source.isValid() && source.isSimple()) {
                return source;
            }

            // validate coordinates, remove empty shell/holes, duplicated points
            Geometry valid = validateEmptyAndDuplicate(source);
            if (valid == null || valid.isEmpty()) {
                return null;
            }

            if (valid.isValid() && valid.isSimple()) {
                return valid;
            }

            // reconstruct self-intersection geometry
            Class<?> geomBinding = valid.getClass();
            if (geomBinding.isAssignableFrom(MultiPolygon.class)) {
                return validatePolygon(valid);
            } else if (geomBinding.isAssignableFrom(Polygon.class)) {
                return validatePolygon(valid);
            } else if (geomBinding.isAssignableFrom(MultiLineString.class)) {
                return validateLineString(valid);
            } else if (geomBinding.isAssignableFrom(LineString.class)) {
                return validateLineString(valid);
            } else {
                Assert.shouldNeverReachHere(source.toText());
            }

            return null;
        }

        private Geometry validatePolygon(Geometry source) {
            Polygonizer polygonizer = new Polygonizer();
            for (int index = 0; index < source.getNumGeometries(); index++) {
                Polygon polygon = (Polygon) source.getGeometryN(index);
                visitLinearRing(polygonizer, polygon.getExteriorRing());
                for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
                    visitLinearRing(polygonizer, polygon.getInteriorRingN(n));
                }
            }

            @SuppressWarnings("unchecked")
            Collection<Polygon> polygons = polygonizer.getPolygons();
            if (polygons.size() == 0) {
                return null;
            }

            if (polygons.size() == 1) {
                Polygon polygon = polygons.iterator().next();
                return source.getFactory().createMultiPolygon(new Polygon[] { polygon });
            }

            Iterator<Polygon> iter = polygons.iterator();
            Geometry repaired = iter.next();
            while (iter.hasNext()) {
                repaired = repaired.symDifference(iter.next());
            }
            return repaired;
        }

        private void visitLinearRing(Polygonizer polygonizer, LineString line) {
            GeometryFactory factory = line.getFactory();
            if (line instanceof LinearRing) {
                line = factory.createLineString(line.getCoordinateSequence());
            }

            // set dirty
            Geometry union = line.union(factory.createPoint(line.getCoordinateN(0)));

            polygonizer.add(union);
        }

        private MultiLineString validateLineString(Geometry line) {
            GeometryFactory factory = line.getFactory();

            // set dirty
            Geometry union = line.union(factory.createPoint(line.getCoordinate()));

            final List<LineString> lines = new ArrayList<LineString>();
            union.apply(new GeometryComponentFilter() {
                @Override
                public void filter(Geometry geom) {
                    if (geom instanceof LineString) {
                        if (!geom.isEmpty()) {
                            lines.add((LineString) geom);
                        }
                    }
                }
            });

            if (lines.size() == 0) {
                return null;
            }

            return factory.createMultiLineString(lines.toArray(new LineString[0]));
        }

        private Geometry validateEmptyAndDuplicate(Geometry source) {
            GeometryFactory factory = source.getFactory();

            Class<?> geomBinding = source.getClass();
            if (geomBinding.isAssignableFrom(MultiPolygon.class)) {
                List<Polygon> polygons = new ArrayList<Polygon>();
                for (int idx = 0; idx < source.getNumGeometries(); idx++) {
                    Polygon edit = (Polygon) validateEmptyAndDuplicate(source.getGeometryN(idx));
                    if (edit == null) {
                        continue;
                    }
                    polygons.add(edit);
                }

                if (polygons.size() == 0) {
                    return null;
                }

                return factory.createMultiPolygon(polygons.toArray(new Polygon[0]));
            } else if (geomBinding.isAssignableFrom(Polygon.class)) {
                Polygon polygon = (Polygon) source;

                // exterior ring
                LineString exterior = validateRing(
                        (LineString) validateEmptyAndDuplicate(polygon.getExteriorRing()));
                if (exterior == null || exterior.isEmpty() || exterior.getLength() == 0) {
                    return null;
                }
                LinearRing shell = factory.createLinearRing(exterior.getCoordinateSequence());

                // interior ring
                List<LinearRing> holes = new ArrayList<LinearRing>();
                for (int idx = 0; idx < polygon.getNumInteriorRing(); idx++) {
                    LineString interior = validateRing(
                            (LineString) validateEmptyAndDuplicate(polygon.getInteriorRingN(idx)));
                    if (interior == null || interior.isEmpty() || interior.getLength() == 0) {
                        continue;
                    }

                    holes.add(factory.createLinearRing(interior.getCoordinateSequence()));
                }

                return factory.createPolygon(shell, holes.toArray(new LinearRing[0]));
            } else if (geomBinding.isAssignableFrom(MultiLineString.class)) {
                List<LineString> lines = new ArrayList<LineString>();
                for (int idx = 0; idx < source.getNumGeometries(); idx++) {
                    LineString edit = (LineString) validateEmptyAndDuplicate(
                            (LineString) source.getGeometryN(idx));
                    if (edit == null) {
                        continue;
                    }
                    lines.add(edit);
                }

                if (lines.size() == 0) {
                    return null;
                }

                return factory.createMultiLineString(lines.toArray(new LineString[0]));
            } else if (geomBinding.isAssignableFrom(LineString.class)) {
                List<Coordinate> coordList = new ArrayList<Coordinate>();
                Coordinate[] coords = source.getCoordinates();
                for (int idx = 1, length = coords.length - 1; idx < length; idx++) {
                    Coordinate coordinate = coords[idx];
                    if (!isValid(coordinate) || coordList.contains(coordinate)) {
                        continue;
                    }
                    coordList.add(coordinate);
                }

                // first coordinate
                if (coordList.size() == 0 || !coordList.get(0).equals(coords[0])) {
                    coordList.add(0, coords[0]);
                }

                // last coordinate
                if (!coordList.get(coordList.size() - 1).equals(coords[coords.length - 1])) {
                    coordList.add(coords[coords.length - 1]);
                }

                if (coordList.size() < 2) {
                    return null;
                }

                return factory.createLineString(CoordinateArrays.toCoordinateArray(coordList));
            } else if (geomBinding.isAssignableFrom(LinearRing.class)) {
                LineString line = factory.createLineString(source.getCoordinates());
                return validateEmptyAndDuplicate(line);
            } else if (geomBinding.isAssignableFrom(MultiPoint.class)) {
                List<Coordinate> coordList = new ArrayList<Coordinate>();
                for (Coordinate coordinate : source.getCoordinates()) {
                    if (!isValid(coordinate) || coordList.contains(coordinate)) {
                        continue;
                    }
                    coordList.add(coordinate);
                }

                return factory
                        .createMultiPointFromCoords(CoordinateArrays.toCoordinateArray(coordList));
            } else if (geomBinding.isAssignableFrom(Point.class)) {
                if (isValid(source.getCoordinate())) {
                    return source;
                }
            }

            return null;
        }

        private LineString validateRing(LineString ring) {
            if (ring == null || ring.isEmpty() || ring.getLength() == 0) {
                return null;
            }

            GeometryFactory factory = ring.getFactory();
            if (!ring.isClosed()) {
                List<Coordinate> sources = Arrays.asList(ring.getCoordinates());
                List<Coordinate> coordList = new ArrayList<Coordinate>(sources);
                coordList.add(coordList.get(0));
                ring = factory.createLineString(coordList.toArray(new Coordinate[0]));
            }

            return factory.createLinearRing(ring.getCoordinateSequence());
        }

        private boolean isValid(Coordinate coord) {
            if (Double.isNaN(coord.x) || Double.isInfinite(coord.x)) {
                return false;
            }

            if (Double.isNaN(coord.y) || Double.isInfinite(coord.y)) {
                return false;
            }
            return true;
        }
    }
}