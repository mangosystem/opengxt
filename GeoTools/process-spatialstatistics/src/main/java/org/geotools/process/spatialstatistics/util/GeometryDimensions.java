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
package org.geotools.process.spatialstatistics.util;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Coordinates;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

/**
 * Force the coordinate dimension(XY, XYZ, XYM, XYZM) of a geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryDimensions {
    protected static final Logger LOGGER = Logging.getLogger(GeometryDimensions.class);

    public enum DimensionType {
        /*
         * CoordinateXY
         */
        XY,

        /*
         * Coordinate
         */
        XYZ,

        /*
         * CoordinateXYM
         */
        XYM,

        /*
         * CoordinateXYZM
         */
        XYZM
    }

    public static Geometry force(Geometry geometry, DimensionType dimension) {
        return force(geometry, dimension, null, null);
    }

    public static Geometry force(Geometry geometry, DimensionType dimension, Double zValue) {
        return force(geometry, dimension, zValue, null);
    }

    public static Geometry force(Geometry geometry, DimensionType dimension, Double zValue,
            Double mValue) {
        GeometryFactory gf = geometry.getFactory();
        Geometry g = geometry;

        if (geometry instanceof Point) {
            g = gf.createPoint(convert(geometry.getCoordinates(), dimension, zValue, mValue));
        } else if (geometry instanceof LineString) {
            g = gf.createLineString(convert(geometry.getCoordinates(), dimension, zValue, mValue));
        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;

            Coordinate[] ecoords = polygon.getExteriorRing().getCoordinates();
            LinearRing shell = gf.createLinearRing(convert(ecoords, dimension, zValue, mValue));

            final LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                Coordinate[] icoords = polygon.getInteriorRingN(i).getCoordinates();
                holes[i] = gf.createLinearRing(convert(icoords, dimension, zValue, mValue));
            }

            g = gf.createPolygon(shell, holes);
        } else if (geometry instanceof MultiPoint) {
            g = gf.createMultiPoint(convert(geometry.getCoordinates(), dimension, zValue, mValue));
        } else if (geometry instanceof MultiLineString) {
            MultiLineString mls = (MultiLineString) geometry;

            final LineString[] ls = new LineString[mls.getNumGeometries()];
            for (int i = 0; i < mls.getNumGeometries(); i++) {
                ls[i] = (LineString) force(mls.getGeometryN(i), dimension);
            }

            g = gf.createMultiLineString(ls);
        } else if (geometry instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) geometry;

            final Polygon[] pl = new Polygon[mp.getNumGeometries()];
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                pl[i] = (Polygon) force(mp.getGeometryN(i), dimension);
            }

            g = gf.createMultiPolygon(pl);
        } else if (geometry instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) geometry;

            final Geometry[] geometries = new Geometry[gc.getNumGeometries()];
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                geometries[i] = force(gc.getGeometryN(i), dimension);
            }

            g = gf.createGeometryCollection(geometries);
        }

        g.setUserData(geometry.getUserData());

        return g;
    }

    private static int getDimension(DimensionType dimension) {
        if (dimension == DimensionType.XYM || dimension == DimensionType.XYZ) {
            return 3;
        } else if (dimension == DimensionType.XYZM) {
            return 4;
        }

        return 2;
    }

    private static CoordinateArraySequence convert(Coordinate[] coordinates,
            DimensionType dimensionType, Double zValue, Double mValue) {
        final int dimension = getDimension(dimensionType);

        boolean supportM = dimensionType == DimensionType.XYM
                || dimensionType == DimensionType.XYZM;
        final int measures = supportM ? 1 : 0;

        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coord = coordinates[i];

            Coordinate coordinate = Coordinates.create(dimension, measures);
            coordinate.setCoordinate(coord);

            switch (dimensionType) {
            case XY: // CoordinateXY(2)
                // skip
                break;
            case XYZ: // Coordinate(3, 0)
                if (zValue == null || zValue.isNaN()) {
                    if (Double.isNaN(coord.getZ())) {
                        coordinate.setZ(0);
                    }
                } else {
                    coordinate.setZ(zValue);
                }
                break;
            case XYM: // CoordinateXYM(3, 1)
                if (mValue == null || mValue.isNaN()) {
                    if (Double.isNaN(coord.getM())) {
                        coordinate.setM(0);
                    }
                } else {
                    coordinate.setM(mValue);
                }
                break;
            case XYZM: // CoordinateXYZM(4, 1)
                if (zValue == null || zValue.isNaN()) {
                    if (Double.isNaN(coord.getZ())) {
                        coordinate.setZ(0);
                    } else {
                        coordinate.setZ(coord.getZ());
                    }
                } else {
                    coordinate.setZ(zValue);
                }

                if (mValue == null || mValue.isNaN()) {
                    if (Double.isNaN(coord.getM())) {
                        coordinate.setM(0);
                    } else {
                        coordinate.setM(coord.getM());
                    }
                } else {
                    coordinate.setM(mValue);
                }
                break;
            }

            coordinates[i] = coordinate;
        }
        return new CoordinateArraySequence(coordinates, dimension);
    }
}
