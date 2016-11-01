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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Performs rectangular binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class RectangularBinningOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RectangularBinningOperation.class);

    static final String UID = "uid";

    static final String AGG_FIELD = "val";

    private Boolean onlyValidGrid = Boolean.TRUE;

    public Boolean getOnlyValidGrid() {
        return onlyValidGrid;
    }

    public void setOnlyValidGrid(Boolean onlyValidGrid) {
        this.onlyValidGrid = onlyValidGrid;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            ReferencedEnvelope bbox, Double width, Double height) throws IOException {
        return execute(features, null, bbox, width, height);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox, Double width, Double height) throws IOException {
        if (bbox == null) {
            throw new NullPointerException("bbox parameter is null");
        }

        int columns = (int) Math.floor((bbox.getWidth() / width) + 0.5d);
        int rows = (int) Math.floor((bbox.getHeight() / height) + 0.5d);

        columns = columns * width < bbox.getWidth() ? columns + 1 : columns;
        rows = rows * height < bbox.getHeight() ? rows + 1 : rows;

        // recalculate envelope : origin = lower left
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        ReferencedEnvelope finalBBox = new ReferencedEnvelope(targetCRS);
        finalBBox.init(bbox.getMinX(), bbox.getMinX() + (columns * width), bbox.getMinY(),
                bbox.getMinY() + (rows * height));
        return execute(features, weight, bbox, columns, rows);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            ReferencedEnvelope bbox, Integer columns, Integer rows) throws IOException {
        return execute(features, null, bbox, columns, rows);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
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
        SimpleFeatureType schema = FeatureTypes.getDefaultType("Binning", Polygon.class, sourceCRS);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);
        schema = FeatureTypes.add(schema, AGG_FIELD, Double.class, 38);

        final double width = bbox.getWidth() / columns;
        final double height = bbox.getHeight() / rows;

        final double minX = bbox.getMinX();
        final double minY = bbox.getMinY();

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
                    try {
                        geometry = transformer.transform(geometry);
                    } catch (TransformException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }
                }
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                // origin = lower left
                int col = (int) Math.floor((coordinate.x - minX) / width);
                int row = (int) Math.floor((coordinate.y - minY) / height);
                if (col < 0 || row < 0 || col >= columns || row >= rows) {
                    continue;
                }

                Double preVal = gridValues[row][col];
                gridValues[row][col] = preVal == null ? val : preVal + val;
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

            int featureID = 0;
            ReferencedEnvelope bounds = new ReferencedEnvelope(targetCRS);
            double ypos = minY;
            for (int row = 0; row < rows; row++) {
                double xpos = minX;
                for (int col = 0; col < columns; col++) {
                    Double gridValue = gridValues[row][col];
                    if (gridValue == null && onlyValidGrid) {
                        xpos += width;
                        continue;
                    }

                    bounds.init(xpos, xpos + width, ypos, ypos + height);
                    Geometry grid = gf.toGeometry(bounds);
                    if (transform != null) {
                        // reproject grid geometry to sourceCRS
                        try {
                            grid = transformer.transform(grid);
                        } catch (TransformException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }
                    }

                    // create feature and set geometry
                    SimpleFeature newFeature = featureWriter.buildFeature(null);
                    newFeature.setAttribute(UID, ++featureID);
                    newFeature.setAttribute(AGG_FIELD, gridValue);
                    newFeature.setDefaultGeometry(grid);

                    featureWriter.write(newFeature);
                    xpos += width;
                }
                ypos += height;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS, boolean lenient) {
        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            return null;
        }

        try {
            return CRS.findMathTransform(sourceCRS, targetCRS, lenient);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Could not create math transform");
        }
    }
}
