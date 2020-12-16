/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Calculates the volume change between two surfaces.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFillOperation2 extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillOperation2.class);

    public RasterCutFillOperation2() {

    }

    public SimpleFeatureCollection execute(GridCoverage2D beforeDEM, GridCoverage2D afterDEM,
            Geometry cropShape) throws ProcessException, IOException {
        if (cropShape == null || cropShape.isEmpty()) {
            throw new ProcessException("cropShape is null or empty!");
        }

        if (!validateProperties(beforeDEM, afterDEM)) {
            throw new ProcessException(
                    "beforeDEM and afterDEM must have the same coordinate system and cell size!");
        }

        // check cell size
        RasterClipOperation cropOp = new RasterClipOperation();
        GridCoverage2D beforeGc = cropOp.execute(beforeDEM, cropShape);
        GridCoverage2D afterGc = cropOp.execute(afterDEM, cropShape);

        final double beforeNoData = RasterHelper.getNoDataValue(beforeGc);
        final double afaterNoData = RasterHelper.getNoDataValue(afterGc);

        GridGeometry2D beforeGG2D = beforeDEM.getGridGeometry();
        AffineTransform beforeTrans = (AffineTransform) beforeGG2D.getGridToCRS2D();

        double beforeX = Math.abs(beforeTrans.getScaleX());
        double beforeY = Math.abs(beforeTrans.getScaleY());

        final double cellArea = beforeX * beforeY;
        CutFillResult result = new CutFillResult(0d);

        PlanarImage inputImage = (PlanarImage) beforeGc.getRenderedImage();
        PlanarImage afterImage = (PlanarImage) afterGc.getRenderedImage();

        RectIter beforeIter = RectIterFactory.create(inputImage, inputImage.getBounds());
        RectIter afterIter = RectIterFactory.create(afterImage, afterImage.getBounds());

        beforeIter.startLines();
        afterIter.startLines();
        while (!beforeIter.finishedLines() && !afterIter.finishedLines()) {
            beforeIter.startPixels();
            afterIter.startPixels();

            while (!beforeIter.finishedPixels() && !afterIter.finishedPixels()) {
                final double beforeVal = beforeIter.getSampleDouble(0);
                final double afterVal = afterIter.getSampleDouble(0);

                if (SSUtils.compareDouble(beforeNoData, beforeVal)
                        || SSUtils.compareDouble(afaterNoData, afterVal)) {
                    beforeIter.nextPixel();
                    afterIter.nextPixel();
                    continue;
                }

                double diffVal = beforeVal - afterVal;
                double volume = Math.abs(cellArea * diffVal);

                // Cut = 1, Fill = -1, Unchanged = 0
                if (diffVal > 0) {
                    result.cutArea += cellArea;
                    result.cutVolume += volume;
                    result.cutCount += 1;
                } else if (diffVal < 0) {
                    result.fillArea += cellArea;
                    result.fillVolume += volume;
                    result.fillCount += 1;
                } else {
                    result.unChangedArea += cellArea;
                    result.unChangedCount += 1;
                }

                beforeIter.nextPixel();
                afterIter.nextPixel();
            }

            beforeIter.nextLine();
            afterIter.nextLine();
        }

        // finally build features
        return buildFeatures(result, beforeDEM.getCoordinateReferenceSystem());
    }

    private SimpleFeatureCollection buildFeatures(CutFillResult result,
            CoordinateReferenceSystem crs) throws ProcessException, IOException {
        // create schema
        SimpleFeatureType schema = FeatureTypes.getDefaultType("CutFill", Polygon.class, crs);
        schema = FeatureTypes.add(schema, "category", Integer.class);
        schema = FeatureTypes.add(schema, "count", Integer.class);
        schema = FeatureTypes.add(schema, "area", Double.class);
        schema = FeatureTypes.add(schema, "volume", Double.class);

        // create features
        ListFeatureCollection outputFc = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        int serialID = 0;
        for (int category = -1; category <= 1; category++) {
            double area = result.getArea(category);
            double volume = result.getVolume(category);
            int count = result.getCount(category);

            String fid = schema.getTypeName() + "." + (++serialID);
            SimpleFeature newFeature = builder.buildFeature(fid);

            newFeature.setAttribute("category", category);
            newFeature.setAttribute("count", count);
            newFeature.setAttribute("area", area);
            newFeature.setAttribute("volume", volume);

            outputFc.add(newFeature);
        }

        return outputFc;
    }

    private boolean validateProperties(GridCoverage2D beforeDEM, GridCoverage2D afterDEM) {
        CoordinateReferenceSystem beforeCRS = beforeDEM.getCoordinateReferenceSystem();
        CoordinateReferenceSystem afterCRS = afterDEM.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(beforeCRS, afterCRS)) {
            return false;
        }

        GridGeometry2D beforeGG2D = beforeDEM.getGridGeometry();
        AffineTransform beforeTrans = (AffineTransform) beforeGG2D.getGridToCRS2D();

        double beforeX = Math.abs(beforeTrans.getScaleX());
        double beforeY = Math.abs(beforeTrans.getScaleY());

        GridGeometry2D afterGG2D = afterDEM.getGridGeometry();
        AffineTransform afterTrans = (AffineTransform) afterGG2D.getGridToCRS2D();

        double afterX = Math.abs(afterTrans.getScaleX());
        double afterY = Math.abs(afterTrans.getScaleY());

        if (!SSUtils.compareDouble(beforeX, afterX) || !SSUtils.compareDouble(beforeY, afterY)) {
            return false;
        }

        return true;
    }

    static final class CutFillResult {
        public Double baseHeight = Double.NaN;

        public Double cutArea = Double.valueOf(0);

        public Double fillArea = Double.valueOf(0);

        public Double unChangedArea = Double.valueOf(0);

        public Double cutVolume = Double.valueOf(0);

        public Double fillVolume = Double.valueOf(0);

        public Integer cutCount = Integer.valueOf(0);

        public Integer fillCount = Integer.valueOf(0);

        public Integer unChangedCount = Integer.valueOf(0);

        public CutFillResult(Double baseHeight) {
            this.baseHeight = baseHeight;
        }

        public double getArea(int category) {
            if (category == -1) { // fill
                return fillArea;
            } else if (category == 1) { // cut
                return cutArea;
            } else { // unchanged
                return unChangedArea;
            }
        }

        public double getVolume(int category) {
            if (category == -1) { // fill
                return fillVolume;
            } else if (category == 1) { // cut
                return cutVolume;
            } else { // unchanged
                return 0d;
            }
        }

        public int getCount(int category) {
            if (category == -1) { // fill
                return fillCount;
            } else if (category == 1) { // cut
                return cutCount;
            } else { // unchanged
                return unChangedCount;
            }
        }
    }
}