/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2022, Open Source Geospatial Foundation (OSGeo)
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
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Calculates the surface area in polygons using DEM.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterSurfaceAreaOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterSurfaceAreaOperation.class);

    private final double proximalTolerance = 0.0d;

    public RasterSurfaceAreaOperation() {

    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Geometry cropShape)
            throws ProcessException, IOException {
        return execute(inputDEM, 0, cropShape);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Integer bandIndex,
            Geometry cropShape) throws ProcessException, IOException {
        if (cropShape == null || cropShape.isEmpty() || cropShape.getArea() == 0d) {
            throw new ProcessException("cropShape is null or empty!");
        }

        // 0. crop gridcoverage
        GridCoverage2D coverage = preProcess(inputDEM, cropShape);

        // 1. extract points
        final double noData = RasterHelper.getNoDataValue(coverage);
        GridTransformer trans = new GridTransformer(coverage);

        PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
        java.awt.Rectangle inputBounds = inputImage.getBounds();
        RectIter readIter = RectIterFactory.create(inputImage, inputBounds);

        List<Coordinate> coordinateList = new ArrayList<Coordinate>();

        int row = 0; // inputBounds.y
        readIter.startLines();
        while (!readIter.finishedLines()) {
            int column = 0; // inputBounds.x
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                double sampleValue = readIter.getSampleDouble(bandIndex);
                if (!SSUtils.compareDouble(noData, sampleValue)) {
                    Coordinate coordinate = trans.gridToWorldCoordinate(column, row);
                    coordinate.setZ(sampleValue);
                    coordinateList.add(coordinate);
                }
                column++;
                readIter.nextPixel();
            }
            row++;
            readIter.nextLine();
        }

        // Gets the faces of the computed triangulation as a GeometryCollection of Polygon.
        DelaunayTriangulationBuilder vdBuilder = new DelaunayTriangulationBuilder();
        vdBuilder.setTolerance(proximalTolerance);
        vdBuilder.setSites(coordinateList);
        Geometry triangleGeoms = vdBuilder.getTriangles(cropShape.getFactory());
        coordinateList.clear();

        // Calculate surface area
        double surfaceArea = 0d;
        for (int index = 0; index < triangleGeoms.getNumGeometries(); index++) {
            Geometry triangle = triangleGeoms.getGeometryN(index);
            if (triangle == null || triangle.isEmpty()) {
                continue;
            }

            Coordinate[] coords = triangle.getCoordinates();

            // https://mste.illinois.edu/dildine/tcd_files/program17.htm
            // A triangle has sides a, b, and c.
            // After Calculating S, where S = (a+b+c)/2
            // The Area of a Triangle = SQRT(s*(s-a)(s-b)(s-c))

            double a = getSurfaceLength(coords[0], coords[1]);
            double b = getSurfaceLength(coords[1], coords[2]);
            double c = getSurfaceLength(coords[2], coords[0]);
            double S = (a + b + c) / 2.0;

            surfaceArea += Math.sqrt(S * (S - a) * (S - b) * (S - c));
        }

        // 2. write polygon
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("SurfaceArea", Polygon.class,
                crs);
        featureType = FeatureTypes.add(featureType, "area", Double.class, 19);
        featureType = FeatureTypes.add(featureType, "surface", Double.class, 19);
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        try {
            SimpleFeature newFeature = featureWriter.buildFeature();
            newFeature.setAttribute("area", cropShape.getArea());
            newFeature.setAttribute("surface", surfaceArea);
            newFeature.setDefaultGeometry(cropShape);
            featureWriter.write(newFeature);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private double getSurfaceLength(Coordinate a, Coordinate b) {
        // Pythagorean theorem
        double a2 = Math.pow(a.distance(b), 2);
        double b2 = Math.pow(Math.abs(a.getZ() - b.getZ()), 2);
        return Math.sqrt(a2 + b2);
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
}