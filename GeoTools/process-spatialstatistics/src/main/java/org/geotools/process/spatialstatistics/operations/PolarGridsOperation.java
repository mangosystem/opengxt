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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RadialType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a radial polar grids from geometry(centroid) or features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolarGridsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PolarGridsOperation.class);

    // http://gis.stackexchange.com/questions/31274/polar-grids-in-arcgis

    static final int DEFAULT_SIDES = 8;

    static final int DEFAULT_SEGS = 8;

    static final String ANGLE_FIELD = "angle";

    static final String RADIUS_FIELD = "radius";

    static final String AZIMUTH_FIELD = "azimuth";

    private boolean is8Sides = false;

    private RadialType radialType = RadialType.Polar;

    private boolean outsideOnly = true;

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
        this.radialType = radialType;
        this.is8Sides = sides == 8;

        // sort arrays
        Arrays.sort(radius);

        SimpleFeatureType inputSchema = features.getSchema();
        SimpleFeatureType featureType = FeatureTypes.build(inputSchema, getOutputTypeName(),
                Polygon.class);
        featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
        featureType = FeatureTypes.add(featureType, RADIUS_FIELD, Double.class, 38);
        if (sides == 8) {
            featureType = FeatureTypes.add(featureType, AZIMUTH_FIELD, String.class, 5);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
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

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(getOutputTypeName(),
                Polygon.class, crs);
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
                newFeature.setDefaultGeometry(current.clone());
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

        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        double radian;

        // first interior rings
        for (int index = 0; index <= DEFAULT_SEGS; index++) {
            if (index == 0) {
                radian = SSUtils.convert2Radians(fromDeg);
            } else if (index == DEFAULT_SEGS) {
                radian = SSUtils.convert2Radians(toDeg);
            } else {
                radian = SSUtils.convert2Radians(fromDeg + (index * step));
            }
            coordinates.add(createCoordinate(source, radian, fromRadius));
        }

        // second outer rings
        for (int index = DEFAULT_SEGS; index >= 0; index--) {
            if (index == 0) {
                radian = SSUtils.convert2Radians(fromDeg);
            } else if (index == DEFAULT_SEGS) {
                radian = SSUtils.convert2Radians(toDeg);
            } else {
                radian = SSUtils.convert2Radians(fromDeg + (index * step));
            }
            coordinates.add(createCoordinate(source, radian, toRadius));
        }

        // close rings
        coordinates.add(coordinates.get(0));

        // create polygon
        Coordinate[] coords = new Coordinate[coordinates.size()];
        coordinates.toArray(coords);

        return gf.createPolygon(gf.createLinearRing(coords), null);
    }

    private Geometry createCircularArc(Coordinate source, double fromDeg, double toDeg,
            double radius) {
        final double step = Math.abs(toDeg - fromDeg) / (double) DEFAULT_SEGS;

        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        coordinates.add(source);

        double radian;
        for (int index = DEFAULT_SEGS; index >= 0; index--) {
            if (index == 0) {
                radian = SSUtils.convert2Radians(fromDeg);
            } else if (index == DEFAULT_SEGS) {
                radian = SSUtils.convert2Radians(toDeg);
            } else {
                radian = SSUtils.convert2Radians(fromDeg + (index * step));
            }
            coordinates.add(createCoordinate(source, radian, radius));
        }

        // close rings
        coordinates.add(coordinates.get(0));

        // create polygon
        Coordinate[] coords = new Coordinate[coordinates.size()];
        coordinates.toArray(coords);

        return gf.createPolygon(gf.createLinearRing(coords), null);
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
