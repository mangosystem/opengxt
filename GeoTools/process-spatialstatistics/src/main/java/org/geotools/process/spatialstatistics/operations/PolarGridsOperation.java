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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.enumeration.RadialType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import si.uom.SI;

/**
 * Creates a radial polar grids from geometry(centroid) or features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolarGridsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PolarGridsOperation.class);

    static final String TYPE_NAME = "PolarGrids";

    static final int DEFAULT_SIDES = 8;

    static final int DEFAULT_SEGS = 24;

    static final String ANGLE_FIELD = "angle";

    static final String RADIUS_FIELD = "radius";

    static final String AZIMUTH_FIELD = "azimuth";

    private boolean is8Sides = false;

    private RadialType radialType = RadialType.Polar;

    private boolean outsideOnly = true;

    private boolean isGeographicCRS = false;

    private GeodeticBuilder geodetic;

    public RadialType getRadialType() {
        return radialType;
    }

    public void setRadialType(RadialType radialType) {
        this.radialType = radialType;
    }

    public boolean isOutsideOnly() {
        return outsideOnly;
    }

    public void setOutsideOnly(boolean outsideOnly) {
        this.outsideOnly = outsideOnly;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, double[] radius)
            throws IOException {
        return execute(features, radius, DEFAULT_SIDES);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, double[] radius,
            int sides) throws IOException {
        return execute(features, radius, DEFAULT_SIDES, RadialType.Polar);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, double[] radius,
            int sides, RadialType radialType) throws IOException {
        return execute(features, radius, DistanceUnit.Default, DEFAULT_SIDES, RadialType.Polar);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, double[] radius,
            DistanceUnit radiusUnit, int sides, RadialType radialType) throws IOException {
        this.radialType = radialType;
        this.is8Sides = sides == 8;

        // sort arrays
        Arrays.sort(radius);

        SimpleFeatureType inputSchema = features.getSchema();
        SimpleFeatureType featureType = FeatureTypes.build(inputSchema, TYPE_NAME, Polygon.class);
        featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
        featureType = FeatureTypes.add(featureType, RADIUS_FIELD, Double.class, 38);
        if (sides == 8) {
            featureType = FeatureTypes.add(featureType, AZIMUTH_FIELD, String.class, 5);
        }

        CoordinateReferenceSystem crs = inputSchema.getCoordinateReferenceSystem();
        if (crs != null) {
            isGeographicCRS = UnitConverter.isGeographicCRS(crs);
            Unit<Length> targetUnit = UnitConverter.getLengthUnit(crs);

            if (isGeographicCRS) {
                geodetic = new GeodeticBuilder(crs);
                geodetic.setQuadrantSegments(DEFAULT_SEGS);
                for (int idx = 0; idx < radius.length; idx++) {
                    radius[idx] = UnitConverter.convertDistance(radius[idx], radiusUnit, SI.METRE);
                }
            } else {
                for (int idx = 0; idx < radius.length; idx++) {
                    radius[idx] = UnitConverter.convertDistance(radius[idx], radiusUnit,
                            targetUnit);
                }
            }
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                createRadialGrids(featureWriter, feature, geometry.getCentroid(), radius, sides);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }
        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection execute(Geometry center, double[] radius) throws IOException {
        return execute(center, radius, DEFAULT_SIDES);
    }

    public SimpleFeatureCollection execute(Geometry center, double[] radius, int sides)
            throws IOException {
        return execute(center, radius, DEFAULT_SIDES, RadialType.Polar);
    }

    public SimpleFeatureCollection execute(Geometry center, double[] radius, int sides,
            RadialType radialType) throws IOException {
        return execute(center, null, radius, DistanceUnit.Default, DEFAULT_SIDES, RadialType.Polar);
    }

    public SimpleFeatureCollection execute(Geometry center, CoordinateReferenceSystem forcedCRS,
            double[] radius, DistanceUnit radiusUnit, int sides, RadialType radialType)
            throws IOException {
        this.radialType = radialType;
        this.is8Sides = sides == 8;

        // sort arrays
        Arrays.sort(radius);

        // prepare feature type
        CoordinateReferenceSystem crs = null;
        if (center.getUserData() != null
                && center.getUserData() instanceof CoordinateReferenceSystem) {
            crs = (CoordinateReferenceSystem) center.getUserData();
        }

        if (forcedCRS != null) {
            crs = forcedCRS;
        }

        if (crs != null) {
            isGeographicCRS = UnitConverter.isGeographicCRS(crs);
            Unit<Length> targetUnit = UnitConverter.getLengthUnit(crs);

            if (isGeographicCRS) {
                geodetic = new GeodeticBuilder(crs);
                geodetic.setQuadrantSegments(DEFAULT_SEGS);
                for (int idx = 0; idx < radius.length; idx++) {
                    radius[idx] = UnitConverter.convertDistance(radius[idx], radiusUnit, SI.METRE);
                }
            } else {
                for (int idx = 0; idx < radius.length; idx++) {
                    radius[idx] = UnitConverter.convertDistance(radius[idx], radiusUnit,
                            targetUnit);
                }
            }
        }

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, crs);
        featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
        featureType = FeatureTypes.add(featureType, RADIUS_FIELD, Double.class, 38);
        if (sides == 8) {
            featureType = FeatureTypes.add(featureType, AZIMUTH_FIELD, String.class, 5);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        try {
            createRadialGrids(featureWriter, null, center.getCentroid(), radius, sides);
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private void createRadialGrids(IFeatureInserter featureWriter, SimpleFeature refFeature,
            Point center, double[] radius, int sides) throws IOException {
        Coordinate source = center.getCoordinate();
        double stepAngle = 360.0d / (double) sides;
        double halfStep = radialType == RadialType.Base ? 0.0 : stepAngle / 2.0d;

        Geometry current;
        for (int sideIndex = 0; sideIndex < sides; sideIndex++) {
            double fromDeg = halfStep + (sideIndex * stepAngle);
            double toDeg = halfStep + ((sideIndex + 1) * stepAngle);

            for (int idx = 0; idx < radius.length; idx++) {
                // create feature and set geometry
                if (outsideOnly && idx > 0) {
                    current = createCircularArc(source, fromDeg, toDeg, radius[idx - 1],
                            radius[idx]);
                } else {
                    current = createCircularArc(source, fromDeg, toDeg, radius[idx]);
                }

                SimpleFeature newFeature = featureWriter.buildFeature();
                if (refFeature != null) {
                    featureWriter.copyAttributes(refFeature, newFeature, false);
                }
                newFeature.setDefaultGeometry(current.copy());
                newFeature.setAttribute(ANGLE_FIELD, fromDeg);
                newFeature.setAttribute(RADIUS_FIELD, radius[idx]);

                if (is8Sides) {
                    newFeature.setAttribute(AZIMUTH_FIELD, getAzimuth(fromDeg));
                }

                featureWriter.write(newFeature);
            }
        }
    }

    private Geometry createCircularArc(Coordinate source, double fromDeg, double toDeg,
            double fromRadius, double toRadius) {
        final double step = Math.abs(toDeg - fromDeg) / (double) DEFAULT_SEGS;

        CoordinateList coordinates = new CoordinateList();

        // first interior rings
        for (int index = 0; index <= DEFAULT_SEGS; index++) {
            double deg = fromDeg + (index * step);

            if (index == 0) {
                deg = fromDeg;
            } else if (index == DEFAULT_SEGS) {
                deg = toDeg;
            }

            Coordinate coordinate = null;
            if (isGeographicCRS) {
                coordinate = geodetic.getDestination(source, 90 - deg, fromRadius);
            } else {
                double radian = SSUtils.convert2Radians(deg);
                coordinate = createCoordinate(source, radian, fromRadius);
            }

            coordinates.add(coordinate, false);
        }

        // second outer rings
        for (int index = DEFAULT_SEGS; index >= 0; index--) {
            double deg = fromDeg + (index * step);

            if (index == 0) {
                deg = fromDeg;
            } else if (index == DEFAULT_SEGS) {
                deg = toDeg;
            }

            Coordinate coordinate = null;
            if (isGeographicCRS) {
                coordinate = geodetic.getDestination(source, 90 - deg, toRadius);
            } else {
                double radian = SSUtils.convert2Radians(deg);
                coordinate = createCoordinate(source, radian, toRadius);
            }

            coordinates.add(coordinate, false);
        }

        // close rings
        coordinates.add(coordinates.get(0), true);

        // create polygon
        return gf.createPolygon(coordinates.toCoordinateArray());
    }

    private Geometry createCircularArc(Coordinate source, double fromDeg, double toDeg,
            double radius) {
        final double step = Math.abs(toDeg - fromDeg) / (double) DEFAULT_SEGS;

        CoordinateList coordinates = new CoordinateList();
        coordinates.add(source, false);

        for (int index = DEFAULT_SEGS; index >= 0; index--) {
            double deg = fromDeg + (index * step);

            if (index == 0) {
                deg = fromDeg;
            } else if (index == DEFAULT_SEGS) {
                deg = toDeg;
            }

            Coordinate coordinate = null;
            if (isGeographicCRS) {
                coordinate = geodetic.getDestination(source, 90 - deg, radius);
            } else {
                double radian = SSUtils.convert2Radians(deg);
                coordinate = createCoordinate(source, radian, radius);
            }

            coordinates.add(coordinate, false);
        }

        // close rings
        coordinates.add(coordinates.get(0), true);

        // create polygon
        return gf.createPolygon(coordinates.toCoordinateArray());
    }

    private Coordinate createCoordinate(Coordinate source, double radian, double radius) {
        double dx = Math.cos(radian) * radius;
        double dy = Math.sin(radian) * radius;
        return new Coordinate(source.x + dx, source.y + dy);
    }

    private String getAzimuth(double degree) {
        // sector = [Azimuth] 'sector angle in degrees, must be 0,45,90,135,180,225,270,or 315
        degree = degree > 360 ? degree - 360 : degree;

        String azimuth = "";
        if (radialType == RadialType.Base) {
            if (degree >= 0 && degree < 45) {
                azimuth = "NEE";
            } else if (degree >= 45 && degree < 90) {
                azimuth = "NNE";
            } else if (degree >= 90 && degree < 135) {
                azimuth = "NNW";
            } else if (degree >= 135 && degree < 180) {
                azimuth = "NWW";
            } else if (degree >= 180 && degree < 225) {
                azimuth = "SWW";
            } else if (degree >= 225 && degree < 270) {
                azimuth = "SSW";
            } else if (degree >= 270 && degree < 315) {
                azimuth = "SSE";
            } else if (degree >= 315 && degree < 360) {
                azimuth = "SEE";
            }
        } else {
            if (degree >= 22.5 && degree < 67.5) {
                azimuth = "NE";
            } else if (degree >= 67.5 && degree < 112.5) {
                azimuth = "N";
            } else if (degree >= 112.5 && degree < 157.5) {
                azimuth = "NW";
            } else if (degree >= 157.5 && degree < 202.5) {
                azimuth = "W";
            } else if (degree >= 202.5 && degree < 247.5) {
                azimuth = "SW";
            } else if (degree >= 247.5 && degree < 292.5) {
                azimuth = "S";
            } else if (degree >= 292.5 && degree < 337.5) {
                azimuth = "SE";
            } else if (degree >= 337.5 || degree < 22.5) {
                azimuth = "E";
            }
        }
        return azimuth;
    }
}
