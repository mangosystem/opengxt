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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Finds the highest or lowest points for a polygon geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHighLowPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterHighLowPointsOperation.class);

    public enum HighLowType {
        Both, High, Low
    }

    static final String H = "H";

    static final String L = "L";

    public SimpleFeatureCollection execute(GridCoverage2D inputCoverage, Integer bandIndex,
            Geometry cropShape, HighLowType valueType) throws IOException {
        // 0. crop gridcoverage
        GridCoverage2D coverage = preProcess(inputCoverage, cropShape);

        // 1. find points
        final double noData = RasterHelper.getNoDataValue(coverage);
        GridTransformer trans = new GridTransformer(coverage);

        HighLowPosition high = new HighLowPosition(Double.MIN_VALUE);
        HighLowPosition low = new HighLowPosition(Double.MAX_VALUE);

        PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();

        java.awt.Rectangle inputBounds = inputImage.getBounds();
        RectIter readIter = RectIterFactory.create(inputImage, inputBounds);

        int row = 0; // inputBounds.y
        readIter.startLines();
        while (!readIter.finishedLines()) {
            int column = 0; // inputBounds.x
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                double sampleValue = readIter.getSampleDouble(bandIndex);
                if (!SSUtils.compareDouble(noData, sampleValue)) {
                    Coordinate coordinate = trans.gridToWorldCoordinate(column, row);

                    // high
                    if (valueType == HighLowType.High || valueType == HighLowType.Both) {
                        if (high.value == sampleValue) {
                            high.append(coordinate);
                        } else if (high.value < sampleValue) {
                            high.init(sampleValue, coordinate);
                        }
                    }

                    // low
                    if (valueType == HighLowType.Low || valueType == HighLowType.Both) {
                        if (low.value == sampleValue) {
                            low.append(coordinate);
                        } else if (low.value > sampleValue) {
                            low.init(sampleValue, coordinate);
                        }
                    }
                }
                column++;
                readIter.nextPixel();
            }
            row++;
            readIter.nextLine();
        }

        // 2. write points
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("HighLowPoints", Point.class,
                crs);
        featureType = FeatureTypes.add(featureType, "cat", String.class, 10);
        featureType = FeatureTypes.add(featureType, "val", Double.class, 19);
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        try {
            if (valueType == HighLowType.Low || valueType == HighLowType.Both) {
                insertFeatures(featureWriter, L, low);
            }

            if (valueType == HighLowType.High || valueType == HighLowType.Both) {
                insertFeatures(featureWriter, H, high);
            }
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private void insertFeatures(IFeatureInserter featureWriter, String category,
            HighLowPosition item) throws IOException {
        double value = item.value;
        for (Coordinate coord : item.points) {
            SimpleFeature newFeature = featureWriter.buildFeature();
            newFeature.setAttribute("cat", category);
            newFeature.setAttribute("val", Converters.convert(value, Double.class));

            newFeature.setDefaultGeometry(gf.createPoint(coord));

            featureWriter.write(newFeature);
        }
    }

    private GridCoverage2D preProcess(GridCoverage2D inputCoverage, Geometry cropShape) {
        if (cropShape == null) {
            return inputCoverage;
        }

        Object userData = cropShape.getUserData();
        CoordinateReferenceSystem tCrs = inputCoverage.getCoordinateReferenceSystem();
        if (userData == null) {
            cropShape.setUserData(tCrs);
        } else if (userData instanceof CoordinateReferenceSystem) {
            CoordinateReferenceSystem sCrs = (CoordinateReferenceSystem) userData;
            if (!CRS.equalsIgnoreMetadata(sCrs, tCrs)) {
                try {
                    MathTransform transform = CRS.findMathTransform(sCrs, tCrs, true);
                    cropShape = JTS.transform(cropShape, transform);
                    cropShape.setUserData(tCrs);
                } catch (FactoryException e) {
                    throw new IllegalArgumentException("Could not create math transform");
                } catch (MismatchedDimensionException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }

        RasterClipOperation cropOperation = new RasterClipOperation();
        return cropOperation.execute(inputCoverage, cropShape);
    }

    static class HighLowPosition {
        public double value;

        public List<Coordinate> points = new ArrayList<Coordinate>();

        public HighLowPosition(double value) {
            this.value = value;
        }

        public void init(double value, Coordinate coordinate) {
            this.value = value;
            points.clear();
            points.add(coordinate);
        }

        public void append(Coordinate coordinate) {
            points.add(coordinate);
        }
    }
}
