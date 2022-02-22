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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.DifferenceProcess;
import org.geotools.process.spatialstatistics.IntersectProcess;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
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
public class RasterCutFillOperation3 extends AbstractRasterCutFillOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillOperation2.class);

    public RasterCutFillOperation3() {

    }

    public SimpleFeatureCollection execute(GridCoverage2D beforeDEM, GridCoverage2D afterDEM,
            Geometry cropShape, double baseHeight) throws ProcessException, IOException {
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

        final int outputNoData = -9999;
        final double cellArea = beforeX * beforeY;
        CutFillResult result = new CutFillResult(0d);

        PlanarImage beforeImage = (PlanarImage) beforeGc.getRenderedImage();
        PlanarImage afterImage = (PlanarImage) afterGc.getRenderedImage();

        RectIter beforeIter = RectIterFactory.create(beforeImage, beforeImage.getBounds());
        RectIter afterIter = RectIterFactory.create(afterImage, afterImage.getBounds());

        DiskMemImage beforeOutput = createDiskMemImage(beforeGc, RasterPixelType.SHORT);
        DiskMemImage afterOutput = createDiskMemImage(beforeGc, RasterPixelType.SHORT);

        WritableRectIter beforeWriter = RectIterFactory.createWritable(beforeOutput,
                beforeOutput.getBounds());
        WritableRectIter afterWriter = RectIterFactory.createWritable(afterOutput,
                afterOutput.getBounds());

        beforeIter.startLines();
        afterIter.startLines();
        beforeWriter.startLines();
        afterWriter.startLines();
        while (!beforeIter.finishedLines() && !afterIter.finishedLines()
                && !beforeWriter.finishedLines() && !afterWriter.finishedLines()) {
            beforeIter.startPixels();
            afterIter.startPixels();
            beforeWriter.startPixels();
            afterWriter.startPixels();

            while (!beforeIter.finishedPixels() && !afterIter.finishedPixels()
                    && !beforeWriter.finishedPixels() && !afterWriter.finishedPixels()) {
                final double beforeVal = beforeIter.getSampleDouble(0);
                final double afterVal = afterIter.getSampleDouble(0);

                // 1. CutFill
                int beforeFlag = outputNoData;
                int afterFlag = outputNoData;

                if (!SSUtils.compareDouble(beforeNoData, beforeVal)
                        && !SSUtils.compareDouble(afaterNoData, afterVal)) {
                    double diffVal = beforeVal - afterVal;
                    double volume = Math.abs(cellArea * diffVal);

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

                    beforeFlag = getCutFillFlag(beforeVal, baseHeight, outputNoData);
                    afterFlag = getCutFillFlag(afterVal, baseHeight, outputNoData);
                }

                // 2. Display
                // Flag: Cut = 1, Fill = -1, Unchanged = 0
                beforeFlag = beforeFlag == 1 ? beforeFlag : outputNoData;
                afterFlag = afterFlag == 1 ? afterFlag : outputNoData;

                beforeWriter.setSample(0, beforeFlag);
                afterWriter.setSample(0, afterFlag);

                beforeIter.nextPixel();
                afterIter.nextPixel();
                beforeWriter.nextPixel();
                afterWriter.nextPixel();
            }

            beforeIter.nextLine();
            afterIter.nextLine();
            beforeWriter.nextLine();
            afterWriter.nextLine();
        }

        GridCoverage2D beforeCutFill = createGridCoverage("CutFill_Before", beforeOutput, 0,
                outputNoData, -1, 1, gridExtent);

        GridCoverage2D aftreCutFill = createGridCoverage("CutFill_After", afterOutput, 0,
                outputNoData, -1, 1, gridExtent);

        // finally build features
        return buildFeatures(beforeCutFill, aftreCutFill, result);
    }

    private SimpleFeatureCollection buildFeatures(GridCoverage2D beforeCutFill,
            GridCoverage2D aftreCutFill, CutFillResult result)
            throws ProcessException, IOException {
        RasterToPolygonOperation converter = new RasterToPolygonOperation();

        SimpleFeatureCollection beforeFc = converter.execute(beforeCutFill, 0, false, CATEGORY);
        SimpleFeatureCollection afterFc = converter.execute(aftreCutFill, 0, false, CATEGORY);

        SimpleFeatureCollection cutFc = DifferenceProcess.process(beforeFc, afterFc, null);
        SimpleFeatureCollection fillFc = DifferenceProcess.process(afterFc, beforeFc, null);
        SimpleFeatureCollection doneFc = IntersectProcess.process(beforeFc, afterFc, null);

        // create schema
        SimpleFeatureType schema = FeatureTypes.build(beforeFc.getSchema(), "CutFill");
        schema = FeatureTypes.add(schema, CATEGORY, Integer.class);
        schema = FeatureTypes.add(schema, "count", Integer.class);
        schema = FeatureTypes.add(schema, "area", Double.class);
        schema = FeatureTypes.add(schema, "volume", Double.class);

        // create features
        ListFeatureCollection outputFc = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        int serialID = 0;
        for (int category = -1; category <= 1; category++) {
            Geometry unionGeometry = null;

            if (category == -1) {
                unionGeometry = unionGeometry(fillFc);
            } else if (category == 0) {
                unionGeometry = unionGeometry(doneFc);
            } else {
                unionGeometry = unionGeometry(cutFc);
            }

            double area = result.getArea(category);
            double volume = result.getVolume(category);
            int count = result.getCount(category);

            String fid = schema.getTypeName() + "." + (++serialID);
            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(unionGeometry);

            newFeature.setAttribute(CATEGORY, category);
            newFeature.setAttribute("count", count);
            newFeature.setAttribute("area", area);
            newFeature.setAttribute("volume", volume);

            outputFc.add(newFeature);
        }

        return outputFc;
    }

    private Geometry unionGeometry(SimpleFeatureCollection features) {
        Geometry unionGeometry = null;

        if (features == null) {
            return unionGeometry;
        }

        List<Geometry> geometries = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                geometries.add(geometry);
            }
        } finally {
            featureIter.close();
        }

        if (geometries.size() > 0) {
            CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
            unionGeometry = unionOp.union();
        }

        return unionGeometry;
    }

    private int getCutFillFlag(double beforeVal, double afterVal, int outputNoData) {
        // Flag: Cut = 1, Fill = -1, Unchanged = 0
        int flag = outputNoData;

        double diffVal = beforeVal - afterVal;
        if (diffVal > 0) {
            flag = 1;
        } else if (diffVal < 0) {
            flag = -1;
        } else {
            flag = 0;
        }

        return flag;
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
}