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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a ring map from features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RingMapsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RingMapsOperation.class);

    private static final String RING_NUM = "ring_num";

    private static final int DEFAULT_SEGS = 10;

    private int GAPS = 1;

    private SimpleFeatureCollection ringFc;

    private SimpleFeatureCollection anchorFc;

    public SimpleFeatureCollection getRingFc() {
        return ringFc;
    }

    public SimpleFeatureCollection getAnchorFc() {
        return anchorFc;
    }

    public boolean execute(SimpleFeatureCollection inputFeatures, String fields,
            String targetField, Integer ringGap) throws IOException {
        SimpleFeatureType inputSchema = inputFeatures.getSchema();

        GAPS = ringGap >= DEFAULT_SEGS || ringGap < 0 ? 1 : ringGap;

        List<String> valueFields = new ArrayList<String>();
        int ring_num = 0;
        if (fields.matches("^[0-9]+$")) {
            ring_num = Integer.valueOf(fields);
        } else {
            String[] input_fields = fields.split(",");
            for (String field : input_fields) {
                field = FeatureTypes.validateProperty(inputSchema, field.trim());
                if (inputSchema.indexOf(field) != -1) {
                    valueFields.add(field);
                } else {
                    LOGGER.log(Level.INFO, field + " does not exist!");
                }
            }
            ring_num = valueFields.size();
        }

        // feature type
        SimpleFeatureType ringType = FeatureTypes.build(inputSchema, "ringmap", Polygon.class);
        ringType = FeatureTypes.add(ringType, RING_NUM, Integer.class, 5);
        ringType = FeatureTypes.add(ringType, targetField, Double.class, 33);

        SimpleFeatureType anchorType = FeatureTypes.build(inputSchema, "anchor", LineString.class);

        // centroid and bounds
        ReferencedEnvelope bounds = inputFeatures.getBounds();
        double xF = Math.pow(bounds.getMaxX() - bounds.getMinX(), 2.0);
        double yF = Math.pow(bounds.getMaxY() - bounds.getMinY(), 2.0);

        double radius = (Math.pow(xF + yF, 0.5)) / 2.0;
        Coordinate center = bounds.centre();

        List<SimpleFeature> features = this.loadFeatures(inputFeatures);
        int feature_count = features.size();

        double radiusInterval = radius / ring_num;
        double stepAngle = 360.0 / feature_count;

        IFeatureInserter ringWriter = getFeatureWriter(ringType);
        IFeatureInserter anchorWriter = getFeatureWriter(anchorType);
        try {
            for (int idxSide = 0; idxSide < feature_count; idxSide++) {
                double fromDeg = idxSide * stepAngle;
                double toDeg = (idxSide + 1) * stepAngle;
                double defaultRadius = radius;

                SimpleFeature nearestFeature = null;
                for (int idxRadius = 0; idxRadius < ring_num; idxRadius++) {
                    Geometry cell = createCell(center, fromDeg, toDeg, defaultRadius, defaultRadius
                            + radiusInterval);

                    if (idxRadius == 0) {
                        // find nearest feature & create anchor line
                        Point cellCenter = cell.getCentroid();
                        double minDist = Double.MAX_VALUE;
                        for (SimpleFeature feature : features) {
                            Geometry current = (Geometry) feature.getDefaultGeometry();
                            double currentDist = current.distance(cellCenter);
                            if (minDist > currentDist) {
                                nearestFeature = feature;
                                minDist = currentDist;
                            }
                        }

                        // create line
                        double radian = Math.toRadians(fromDeg + (Math.abs(toDeg - fromDeg) / 2));
                        Geometry nearestGeom = (Geometry) nearestFeature.getDefaultGeometry();
                        Geometry line = gf.createLineString(new Coordinate[] {
                                createPoint(center, radian, defaultRadius),
                                nearestGeom.getCentroid().getCoordinate() });

                        SimpleFeature newFeature = anchorWriter.buildFeature();
                        anchorWriter.copyAttributes(nearestFeature, newFeature, false);
                        newFeature.setDefaultGeometry(line);
                        anchorWriter.write(newFeature);

                        // remove this feature
                        features.remove(nearestFeature);
                    }

                    SimpleFeature newFeature = ringWriter.buildFeature();
                    ringWriter.copyAttributes(nearestFeature, newFeature, false);
                    newFeature.setDefaultGeometry(cell);
                    // update attributes
                    newFeature.setAttribute(RING_NUM, idxRadius + 1);

                    if (valueFields.size() == 0) {
                        newFeature.setAttribute(targetField, 0);
                    } else {
                        newFeature.setAttribute(targetField,
                                nearestFeature.getAttribute(valueFields.get(idxRadius)));
                    }

                    ringWriter.write(newFeature);
                    defaultRadius += radiusInterval;
                }
            }
        } catch (Exception e) {
            ringWriter.rollback(e);
            anchorWriter.rollback(e);
            return false;
        } finally {
            ringWriter.close();
            anchorWriter.close();
        }

        this.ringFc = ringWriter.getFeatureCollection();
        this.anchorFc = anchorWriter.getFeatureCollection();
        return true;
    }

    private Geometry createCell(Coordinate centroid, double fromDeg, double toDeg,
            double from_radius, double to_radius) {
        CoordinateList coordinates = new CoordinateList();
        double step = Math.abs(toDeg - fromDeg) / DEFAULT_SEGS;

        // first inner
        for (int index = 0; index < (DEFAULT_SEGS + 1 - GAPS); index++) {
            double radian = Math.toRadians(fromDeg + (index * step));
            coordinates.add(createPoint(centroid, radian, from_radius), false);
        }

        // second outer
        for (int index = (DEFAULT_SEGS - GAPS); index >= 0; index--) {
            double radian = Math.toRadians(fromDeg + (index * step));
            coordinates.add(createPoint(centroid, radian, to_radius), false);
        }

        // close polygon
        coordinates.add(coordinates.getCoordinate(0), false);
        return gf.createPolygon(coordinates.toCoordinateArray());
    }

    private Coordinate createPoint(Coordinate centroid, double radian, double radius) {
        double dx = Math.cos(radian) * radius;
        double dy = Math.sin(radian) * radius;
        return new Coordinate(centroid.x + dx, centroid.y + dy);
    }

    private List<SimpleFeature> loadFeatures(SimpleFeatureCollection inputFeatures)
            throws IOException {
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                features.add(featureIter.next());
            }
        } finally {
            featureIter.close();
        }
        return features;
    }
}
