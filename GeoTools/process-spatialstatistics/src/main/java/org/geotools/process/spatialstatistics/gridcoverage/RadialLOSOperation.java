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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import si.uom.SI;

/**
 * Finds the highest or lowest points for a polygon geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RadialLOSOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RadialLOSOperation.class);

    static final String ANGLE_FIELD = "Angle";

    static final String VALUE_FIELD = "Visible";

    private SimpleFeatureType createTemplateFeatures(CoordinateReferenceSystem crs) {
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("RadialLineOfSight",
                LineString.class, crs);

        featureType = FeatureTypes.add(featureType, ANGLE_FIELD, Double.class, 38);
        featureType = FeatureTypes.add(featureType, VALUE_FIELD, Integer.class, 5);

        return featureType;
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputCoverage, Geometry observerPoint,
            Double observerOffset, Double radius, Integer sides, Boolean useCurvature,
            Boolean useRefraction, Double refractionFactor) {
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = createTemplateFeatures(crs);

        // convert unit
        boolean isGeographic = false;
        if (crs != null) {
            CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
            if (hor instanceof GeographicCRS) {
                isGeographic = true;
            } else {
                Unit<?> unit = hor.getCoordinateSystem().getAxis(0).getUnit();
                UnitConverter converter = SI.METRE.getConverterTo((Unit<Length>) unit);
                radius = converter.convert(radius).doubleValue();
            }
        }

        // prepare transactional feature store
        ListFeatureCollection features = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
        Coordinate center = observerPoint.getCoordinate();

        GeodeticCalculator calculator = null;

        if (isGeographic) {
            calculator = new GeodeticCalculator(crs);
            calculator.setStartingGeographicPoint(center.x, center.y); // lon-lat order
        }

        int serialID = 0;
        for (int index = 0; index < sides; index++) {
            double angle = ((double) index / (double) sides) * Math.PI * 2.0;
            double dx = Math.cos(angle) * radius;
            double dy = Math.sin(angle) * radius;
            Coordinate to = new Coordinate(center.x + dx, center.y + dy);

            if (isGeographic) {
                double azimuth = 360.0 * index / sides - 180;
                calculator.setDirection(azimuth, radius);
                Point2D dp = calculator.getDestinationGeographicPoint();
                to.setOrdinate(0, dp.getX());
                to.setOrdinate(1, dp.getY());
            }

            LineString los = process.getLineOfSight(center, to, observerOffset, useCurvature,
                    useRefraction, refractionFactor);
            if (los == null) {
                continue;
            }

            Map<Integer, Geometry> map = process.mergeLineOfSight(los);
            for (Entry<Integer, Geometry> entry : map.entrySet()) {
                int visible = entry.getKey();
                Geometry lines = entry.getValue();
                for (int idx = 0; idx < lines.getNumGeometries(); idx++) {
                    LineString line = (LineString) lines.getGeometryN(idx);
                    Geometry multiLine = line.getFactory().createMultiLineString(
                            new LineString[] { line });

                    String fid = featureType.getTypeName() + "." + (++serialID);
                    SimpleFeature newFeature = builder.buildFeature(fid);
                    newFeature.setDefaultGeometry(multiLine);
                    newFeature.setAttribute(ANGLE_FIELD, Math.round(angle * (180.0 / Math.PI)));
                    newFeature.setAttribute(VALUE_FIELD, visible);
                    features.add(newFeature);
                }
            }
        }

        return features;
    }
}
