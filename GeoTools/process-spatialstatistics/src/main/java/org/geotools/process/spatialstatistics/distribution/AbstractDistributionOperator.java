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
package org.geotools.process.spatialstatistics.distribution;

import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

/**
 * Abstract Distribution Operator
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractDistributionOperator extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractDistributionOperator.class);

    protected final String ALL = "ALL";

    protected double getValue(SimpleFeature feature, Expression expression, double defaultValue) {
        if (expression == null) {
            return defaultValue;
        }

        Double dblVal = expression.evaluate(feature, Double.class);
        if (dblVal == null || dblVal.isNaN() || dblVal.isInfinite()) {
            return defaultValue;
        } else {
            return dblVal.doubleValue();
        }
    }

    protected String getCaseValue(SimpleFeature feature, Expression expression) {
        if (expression == null) {
            return ALL;
        }

        String caseValue = expression.evaluate(feature, String.class);
        if (caseValue == null || caseValue.isEmpty()) {
            return ALL;
        } else {
            return caseValue;
        }
    }

    /**
     * The center of gravity for a inputGeometry.
     * 
     * @param inputGeometry
     * @return
     */
    protected Coordinate getTrueCentroid(Geometry inputGeometry) {
        // For line and polygon features, feature centroids are used in distance computations.
        // For multipoints, polylines, or polygons with multiple parts, the centroid is computed
        // using the weighted mean center of all feature parts.
        // The weighting for point features is 1, for line features is length, and for polygon
        // features is area.

        double sumX = 0.0;
        double sumY = 0.0;
        double weightSum = 0.0;

        if (inputGeometry instanceof Point) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof LineString) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof Polygon) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof MultiPoint) {
            MultiPoint mp = (MultiPoint) inputGeometry;
            for (int k = 0; k < mp.getNumGeometries(); k++) {
                Coordinate cen = mp.getGeometryN(k).getCoordinate();

                weightSum += 1;
                sumX += cen.x;
                sumY += cen.y;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof MultiLineString) {
            MultiLineString ml = (MultiLineString) inputGeometry;
            for (int k = 0; k < ml.getNumGeometries(); k++) {
                Geometry lineString = ml.getGeometryN(k);
                Coordinate cen = lineString.getCentroid().getCoordinate();
                final double length = lineString.getLength();

                weightSum += length;
                sumX += cen.x * length;
                sumY += cen.y * length;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) inputGeometry;
            for (int k = 0; k < mp.getNumGeometries(); k++) {
                Geometry polygon = mp.getGeometryN(k);
                Coordinate cen = polygon.getCentroid().getCoordinate();
                final double area = polygon.getArea();

                weightSum += area;
                sumX += cen.x * area;
                sumY += cen.y * area;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) inputGeometry;
            for (int k = 0; k < gc.getNumGeometries(); k++) {
                Coordinate cen = getTrueCentroid(gc.getGeometryN(k));

                weightSum += 1;
                sumX += cen.x;
                sumY += cen.y;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        }

        return inputGeometry.getCentroid().getCoordinate();
    }

}
