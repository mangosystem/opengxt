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
package org.geotools.process.spatialstatistics.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

/**
 * 
 * Performs geodetic calculations on an {@linkplain Ellipsoid ellipsoid}. <br>
 * This class encapsulates a generic ellipsoid and calculates the following properties:
 * <p>
 * <ul>
 * <li>Distance and azimuth between two points.</li>
 * <li>Point located at a given distance and azimuth from an other point.</li>
 * <li>Buffer.</li>
 * </ul>
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeodeticBuilder {
    protected static final Logger LOGGER = Logging.getLogger(GeodeticBuilder.class);

    private final GeometryFactory gf = JTSFactoryFinder
            .getGeometryFactory(GeoTools.getDefaultHints());

    // The orthodromic distance in the same units as the ellipsoid axis (meters by default)
    private double maxSegLength = 100000;

    private int numPoints = 100;

    private int quadrantSegments = 12;

    private BufferParameters bufferParameters;

    private final GeodeticCalculator calculator;

    private boolean isLatLonOrder = false;

    private GeometryCoordinateSequenceTransformer transformer;

    public GeodeticBuilder(CoordinateReferenceSystem geographicCRS) {
        this.calculator = new GeodeticCalculator(geographicCRS);
        this.isLatLonOrder = isLatLonOrder(geographicCRS.getCoordinateSystem());
        this.transformer = new GeometryCoordinateSequenceTransformer();
    }

    public int getNumPoints() {
        return numPoints;
    }

    public void setNumPoints(int numPoints) {
        this.numPoints = numPoints;
    }

    public int getQuadrantSegments() {
        return quadrantSegments;
    }

    public void setQuadrantSegments(int quadrantSegments) {
        this.quadrantSegments = quadrantSegments;
    }

    public BufferParameters getBufferParameters() {
        return bufferParameters;
    }

    public void setBufferParameters(BufferParameters bufferParameters) {
        this.bufferParameters = bufferParameters;
    }

    public Coordinate getDestination(Coordinate from, double azimuth, double distance) {
        calculator.setStartingGeographicPoint(from.x, from.y);
        calculator.setDirection(azimuth, distance);

        Point2D point = calculator.getDestinationGeographicPoint();
        if (isLatLonOrder) {
            return new Coordinate(point.getY(), point.getX());
        } else {
            return new Coordinate(point.getX(), point.getY());
        }
    }

    public double getDistance(Point from, Point to) {
        calculator.setStartingGeographicPoint(from.getX(), from.getY());
        calculator.setDestinationGeographicPoint(to.getX(), to.getY());
        return calculator.getOrthodromicDistance();
    }

    public double getDistance(Coordinate from, Coordinate to) {
        calculator.setStartingGeographicPoint(from.x, from.y);
        calculator.setDestinationGeographicPoint(to.x, to.y);
        return calculator.getOrthodromicDistance();
    }

    public Geometry toGeodesicLine(Geometry multiLineString) {
        List<LineString> lines = new ArrayList<LineString>();

        Geometry multiPart = multiLineString;
        Class<?> binding = multiLineString.getClass();
        if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
            multiPart = multiLineString.getBoundary();
        }

        CoordinateList coordinates = new CoordinateList();
        for (int index = 0; index < multiPart.getNumGeometries(); index++) {
            LineString part = (LineString) multiPart.getGeometryN(index);

            Coordinate[] coords = part.getCoordinates();
            for (int i = 0, j = coords.length - 1; i < j; i++) {
                LineString geodesicLine = getGeodesicLine(coords[i], coords[i + 1]);
                coordinates.add(geodesicLine.getCoordinates(), false);
            }

            lines.add(gf.createLineString(coordinates.toCoordinateArray()));
            coordinates.clear();
        }

        return gf.createMultiLineString(GeometryFactory.toLineStringArray(lines));
    }

    public LineString getGeodesicLine(Geometry from, Geometry to) {
        return getGeodesicLine(from.getCentroid(), to.getCentroid());
    }

    public LineString getGeodesicLine(Point from, Point to) {
        return getGeodesicLine(from.getCoordinate(), to.getCoordinate());
    }

    public LineString getGeodesicLine(Coordinate from, Coordinate to) {
        calculator.setStartingGeographicPoint(from.x, from.y);
        calculator.setDestinationGeographicPoint(to.x, to.y);

        List<Point2D> points = calculator.getGeodeticPath(numPoints);

        CoordinateList coords = new CoordinateList();
        for (Point2D point : points) {
            if (isLatLonOrder) {
                coords.add(new Coordinate(point.getY(), point.getX()), false);
            } else {
                coords.add(new Coordinate(point.getX(), point.getY()), false);
            }
        }

        return gf.createLineString(coords.toCoordinateArray());
    }

    public Geometry getGeodesicLine2(Coordinate from, Coordinate to) {
        Geodesic geodesic = Geodesic.WGS84;
        GeodesicData inverse = geodesic.Inverse(from.y, from.x, to.y, to.x);

        double distance = inverse.s12;
        int interval = (int) Math.ceil(distance / maxSegLength);
        if (interval > numPoints) {
            interval = numPoints;
        }

        List<LineString> lines = new ArrayList<LineString>();
        CoordinateList coords = new CoordinateList();

        double lastLon = 0;
        for (int i = 0; i <= interval; i++) {
            GeodesicData direct = geodesic.Direct(inverse.lat1, inverse.lon1, inverse.azi1,
                    (i / (double) interval) * distance);
            if (coords.size() > 0) {
                if (i > 0 && Math.abs(direct.lon2 - lastLon) > 180) {
                    lines.add(gf.createLineString(coords.toCoordinateArray()));
                    coords.clear();
                }
            }

            coords.add(new Coordinate(direct.lon2, direct.lat2), false);
            lastLon = direct.lon2;
        }

        if (coords.size() > 0) {
            lines.add(gf.createLineString(coords.toCoordinateArray()));
        }

        if (lines.size() > 0) {
            return gf.createMultiLineString(GeometryFactory.toLineStringArray(lines));
        } else {
            return gf.createLineString(new Coordinate[] { from, to });
        }
    }

    public Geometry buffer(Coordinate source, double distance) {
        return bufferPoint(source, distance);
    }

    public Geometry buffer(Geometry source, double distance)
            throws FactoryException, TransformException {
        Class<?> binding = source.getClass();
        if (Polygon.class.equals(binding) || MultiPolygon.class.equals(binding)) {
            return bufferAutoUTM(source, distance);
        } else if (LineString.class.equals(binding) || MultiLineString.class.equals(binding)) {
            return bufferAutoUTM(source, distance);
        } else {
            return bufferPoint(source, distance);
        }
    }

    public Geometry bufferAutoUTM(Geometry source, double distanceInMeters)
            throws FactoryException, TransformException {
        Coordinate center = source.getEnvelopeInternal().centre();

        // WGS 84 / Auto UTM: "AUTO:42001, lon, lat"
        String code = String.format("AUTO:42001, %s, %s", center.x, center.y);
        CoordinateReferenceSystem autoCRS = CRS.decode(code);

        MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, autoCRS);

        // WGS84 --> AUTO
        transformer.setMathTransform(transform);
        Geometry projected = transformer.transform(source);

        // Buffer
        Geometry buffered = projected;
        if (bufferParameters == null) {
            buffered = projected.buffer(distanceInMeters);
        } else {
            buffered = BufferOp.bufferOp(projected, distanceInMeters, bufferParameters);
        }

        // AUTO --> WGS84
        transformer.setMathTransform(transform.inverse());

        return transformer.transform(buffered);
    }

    public Geometry bufferPoint(Geometry center, double distance) {
        List<Polygon> polygons = new ArrayList<Polygon>();

        for (int index = 0; index < center.getNumGeometries(); index++) {
            Geometry part = center.getGeometryN(index);
            Coordinate centroid = part.getCentroid().getCoordinate();
            Geometry buffered = bufferPoint(centroid, distance);
            for (int i = 0; i < buffered.getNumGeometries(); i++) {
                polygons.add((Polygon) buffered.getGeometryN(i));
            }
        }

        return CascadedPolygonUnion.union(polygons);
    }

    public Geometry bufferPoint(Coordinate center, double distance) {
        calculator.setStartingGeographicPoint(center.x, center.y);

        final int size = quadrantSegments * 4;
        CoordinateList coords = new CoordinateList();
        for (int i = 0; i < size; i++) {
            double azimuth = (360.0 * i / size) - 180;

            calculator.setDirection(azimuth, distance);
            Point2D dp = calculator.getDestinationGeographicPoint();
            if (isLatLonOrder) {
                coords.add(new Coordinate(dp.getY(), dp.getX()), false);
            } else {
                coords.add(new Coordinate(dp.getX(), dp.getY()), false);
            }
        }
        coords.closeRing();

        return gf.createPolygon(gf.createLinearRing(coords.toCoordinateArray()), null);
    }

    @SuppressWarnings("unused")
    private Geometry bufferPolygon(Geometry source, double distance) {
        List<Polygon> polygons = new ArrayList<Polygon>();

        for (int index = 0; index < source.getNumGeometries(); index++) {
            Polygon part = (Polygon) source.getGeometryN(index);
            polygons.add(part);

            Geometry buffered = bufferGeodesicLine(part.getBoundary(), distance);
            for (int k = 0; k < buffered.getNumGeometries(); k++) {
                Geometry valid = buffered.getGeometryN(k).buffer(0);
                for (int j = 0; j < valid.getNumGeometries(); j++) {
                    polygons.add((Polygon) valid.getGeometryN(j));
                }
            }
        }

        // return gf.createMultiPolygon(GeometryFactory.toPolygonArray(polygons));
        return CascadedPolygonUnion.union(polygons);
    }

    public Geometry bufferGeodesicLine(Geometry multiLine, double distance) {
        List<Polygon> polygons = new ArrayList<Polygon>();
        for (int index = 0; index < multiLine.getNumGeometries(); index++) {
            LineString line = (LineString) multiLine.getGeometryN(index);
            Coordinate[] coords = line.getCoordinates();
            boolean isLinearRing = line.isClosed();

            int length = coords.length;
            if (length <= 2) {
                line = (LineString) Densifier.densify(line, line.getLength() / 10);
                coords = line.getCoordinates();
                length = coords.length;
            }

            List<Coordinate> hRight = new ArrayList<Coordinate>();
            List<Coordinate> hLeft = new ArrayList<Coordinate>();

            final int segments = quadrantSegments * 2;
            Point2D point = null;
            double angle = 0;
            for (int i = 0; i < length - 1; i++) {
                calculator.setStartingGeographicPoint(coords[i].x, coords[i].y);
                calculator.setDestinationGeographicPoint(coords[i + 1].x, coords[i + 1].y);
                angle = calculator.getAzimuth();

                if (false == isLinearRing && i == 0) {
                    // round buffer
                    for (int k = 0; k < segments; k++) {
                        double azimuth = (angle - 90) - (180.0 * k / segments);
                        calculator.setDirection(azimuth, distance);
                        point = calculator.getDestinationGeographicPoint();
                        hRight.add(new Coordinate(point.getX(), point.getY(), 0));
                    }
                }

                calculator.setDirection(angle + 90, distance);
                point = calculator.getDestinationGeographicPoint();
                hRight.add(new Coordinate(point.getX(), point.getY(), 0));

                calculator.setDirection(angle - 90, distance);
                point = calculator.getDestinationGeographicPoint();
                hLeft.add(new Coordinate(point.getX(), point.getY(), 0));
            }

            if (isLinearRing) {
                // close
                hRight.add(hRight.get(0));
                hLeft.add(hLeft.get(0));
            } else {
                // round buffer
                calculator.setStartingGeographicPoint(coords[length - 1].x, coords[length - 1].y);
                for (int k = 0; k < segments; k++) {
                    double azimuth = (angle + 90) - (180.0 * k / segments);
                    calculator.setDirection(azimuth, distance);
                    point = calculator.getDestinationGeographicPoint();
                    hRight.add(new Coordinate(point.getX(), point.getY(), 0));
                }
            }

            Collections.reverse(hLeft);

            // build polygon
            CoordinateList coordinates = new CoordinateList();
            coordinates.addAll(hRight, false);
            coordinates.addAll(hLeft, false);
            coordinates.closeRing();

            Polygon polygon = gf.createPolygon(gf.createLinearRing(coordinates.toCoordinateArray()),
                    null);
            polygons.add(polygon);
        }

        return gf.createMultiPolygon(GeometryFactory.toPolygonArray(polygons));
    }

    private boolean isLatLonOrder(CoordinateSystem cs) {
        int dimension = cs.getDimension();
        int longitudeDim = -1;
        int latitudeDim = -1;

        for (int i = 0; i < dimension; i++) {
            AxisDirection dir = cs.getAxis(i).getDirection().absolute();

            if (dir.equals(AxisDirection.EAST)) {
                longitudeDim = i;
            }

            if (dir.equals(AxisDirection.NORTH)) {
                latitudeDim = i;
            }
        }

        if ((longitudeDim >= 0) && (latitudeDim >= 0)) {
            if (longitudeDim > latitudeDim) {
                return true;
            }
        }

        return false;
    }

    private MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS, boolean lenient) {
        if (targetCRS == null || CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            return null;
        }

        try {
            return CRS.findMathTransform(sourceCRS, targetCRS, lenient);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Could not create math transform");
        }
    }
}
