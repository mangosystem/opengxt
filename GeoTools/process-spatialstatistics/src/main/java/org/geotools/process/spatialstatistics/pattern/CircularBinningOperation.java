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
package org.geotools.process.spatialstatistics.pattern;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.util.CoordinateTranslateFilter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Performs circular binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class CircularBinningOperation extends BinningOperation {
    protected static final Logger LOGGER = Logging.getLogger(CircularBinningOperation.class);

    static final String TYPE_NAME = "CircularBinning";

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            ReferencedEnvelope bbox, Double radius) throws IOException {
        return execute(features, null, bbox, radius);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox, Double radius) throws IOException {
        if (bbox == null) {
            throw new NullPointerException("bbox parameter is null");
        }

        double diameter = radius * 2.0;

        int columns = (int) Math.floor((bbox.getWidth() / diameter) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / diameter) + 0.5d);

        columns = columns * diameter < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * diameter < bbox.getHeight() ? rows + 1 : rows;

        // recalculate envelope : origin = lower left
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(targetCRS);
        finalBBox.init(bbox.getMinX(), bbox.getMinX() + (columns * diameter), bbox.getMinY(),
                bbox.getMinY() + (rows * diameter));
        return execute(features, weight, bbox, columns, rows);
    }

    private SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox, Integer columns, Integer rows) throws IOException {
        if (bbox == null) {
            throw new NullPointerException("bbox parameter is null");
        }

        // check crs
        CoordinateReferenceSystem sourceCRS = features.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        MathTransform transform = findMathTransform(sourceCRS, targetCRS, true);
        GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();

        // create feature type
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, sourceCRS);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);
        schema = FeatureTypes.add(schema, AGG_FIELD, Double.class, 38);

        final double diameter = bbox.getWidth() / columns;

        final double minX = bbox.getMinX();
        final double minY = bbox.getMinY();

        int minCol = Integer.MAX_VALUE, minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE, maxRow = Integer.MIN_VALUE;

        // calculate grids & values
        Double gridValues[][] = new Double[rows][columns];
        SimpleFeatureIterator featureIter = features.features();
        try {
            if (transform != null) {
                transformer.setMathTransform(transform);
                transformer.setCoordinateReferenceSystem(targetCRS);
            }

            while (featureIter.hasNext()) {
                SimpleFeature feat = featureIter.next();
                Double val = weight == null ? Double.valueOf(1.0) : weight.evaluate(feat,
                        Double.class);
                if (val == null) {
                    continue;
                }

                Geometry geometry = (Geometry) feat.getDefaultGeometry();
                if (transform != null) {
                    // project source geometry to targetCRS
                    geometry = transform(transformer, geometry);
                }
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                // origin = lower left
                int col = (int) Math.floor((coordinate.x - minX) / diameter);
                int row = (int) Math.floor((coordinate.y - minY) / diameter);
                if (col < 0 || row < 0 || col >= columns || row >= rows) {
                    continue;
                }

                Double preVal = gridValues[row][col];
                gridValues[row][col] = preVal == null ? val : preVal + val;

                if (getOnlyValidGrid()) {
                    minCol = Math.min(col, minCol);
                    maxCol = Math.max(col, maxCol);
                    minRow = Math.min(row, minRow);
                    maxRow = Math.max(row, maxRow);
                }
            }
        } finally {
            featureIter.close();
        }

        // write features
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            if (transform != null) {
                transformer.setMathTransform(transform.inverse());
                transformer.setCoordinateReferenceSystem(sourceCRS);
            }

            if (getOnlyValidGrid()) {
                maxCol++;
                maxRow++;
            } else {
                minCol = 0;
                maxCol = columns;
                minRow = 0;
                maxRow = rows;
            }

            final double radius = diameter / 2.0;
            Point center = gf.createPoint(new Coordinate(minX + radius, minY + radius));
            final Geometry circle = center.buffer(radius, quadrantSegments);

            int featureID = 0;
            double ypos = minRow * diameter;
            for (int row = minRow; row < maxRow; row++) {
                double xpos = minCol * diameter;
                for (int col = minCol; col < maxCol; col++) {
                    Double gridValue = gridValues[row][col];
                    if (gridValue == null && getOnlyValidGrid()) {
                        xpos += diameter;
                        continue;
                    }

                    Geometry grid = (Geometry) circle.clone();
                    grid.apply(new CoordinateTranslateFilter(xpos, ypos));
                    grid.setUserData(targetCRS);

                    if (transform != null) {
                        // reproject grid geometry to sourceCRS
                        grid = transform(transformer, grid);
                    }

                    // create feature and set geometry
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setAttribute(UID, ++featureID);
                    newFeature.setAttribute(AGG_FIELD, gridValue);
                    newFeature.setDefaultGeometry(grid);

                    featureWriter.write(newFeature);
                    xpos += diameter;
                }
                ypos += diameter;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
