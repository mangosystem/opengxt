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
import com.vividsolutions.jts.geom.Polygon;

/**
 * Performs circular binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 * @reference http://www.redblobgames.com/grids/hexagons/
 */
public class HexagonalBinningOperation extends BinningOperation {
    protected static final Logger LOGGER = Logging.getLogger(HexagonalBinningOperation.class);

    static final String TYPE_NAME = "HexagonalBinning";

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            ReferencedEnvelope bbox, Double size) throws IOException {
        return execute(features, null, bbox, size);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox, Double size) throws IOException {
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

        final double yoffset = size * 1.5; // height = size * 2
        final double xoffset = Math.sqrt(3.0) * size; // width = sqrt(3)/2 * height
        final double half_xoffset = 0.5 * xoffset;

        int columns = (int) Math.floor((bbox.getWidth() / xoffset) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / yoffset) + 0.5d);

        columns = columns * xoffset < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * yoffset < bbox.getHeight() ? rows + 1 : rows;

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

            final double yOrigin = minY + (size * 0.25);
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
                // TODO: find nearest hexagon
                // Coordinate center = new Coordinate(minX + 0.5 * xoffset, minY + size);
                int row = (int) Math.floor((coordinate.y - yOrigin) / yoffset);
                int col = (int) Math.floor((coordinate.x - minX) / xoffset);
                if ((row % 2) == 1) {
                    col = (int) Math.floor((coordinate.x - minX - half_xoffset) / xoffset);
                }

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

            // create hexagon
            final Geometry hexogon = createHexagon(minX, minY, size);

            int featureID = 0;
            double ypos = minRow * yoffset;
            for (int row = minRow; row < maxRow; row++) {
                double xpos = minCol * xoffset;
                if ((row % 2) == 1) {
                    xpos += half_xoffset;
                }
                for (int col = minCol; col < maxCol; col++) {
                    Double gridValue = gridValues[row][col];
                    if (gridValue == null && getOnlyValidGrid()) {
                        xpos += xoffset;
                        continue;
                    }

                    Geometry grid = (Geometry) hexogon.clone();
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
                    xpos += xoffset;
                }
                ypos += yoffset;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private Geometry createHexagon(double minX, double minY, double size) {
        // center:top --> clockwise
        double span = Math.sqrt(3.0) * size;
        Coordinate[] ring = new Coordinate[7];
        ring[0] = new Coordinate(minX + 0.5 * span, minY + 2.0 * size);
        ring[1] = new Coordinate(minX + span, minY + 1.5 * size);
        ring[2] = new Coordinate(minX + span, minY + 0.5 * size);
        ring[3] = new Coordinate(minX + 0.5 * span, minY);
        ring[4] = new Coordinate(minX, minY + 0.5 * size);
        ring[5] = new Coordinate(minX, minY + 1.5 * size);
        ring[6] = ring[0];
        return gf.createPolygon(gf.createLinearRing(ring), null);
    }
}
