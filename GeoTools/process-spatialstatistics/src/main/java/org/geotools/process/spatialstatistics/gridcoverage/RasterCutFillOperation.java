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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.DataStatistics;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Calculates the volume change between two surfaces.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCutFillOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCutFillOperation.class);

    private GridCoverage2D outputRaster = null;

    private SimpleFeatureCollection outputFeatures = null;

    public RasterCutFillOperation() {

    }

    public GridCoverage2D getOutputRaster() {
        return outputRaster;
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Geometry cropShape)
            throws ProcessException, IOException {
        return execute(inputDEM, cropShape, -9999.0);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, Geometry cropShape,
            double baseHeight) throws ProcessException, IOException {
        RasterCropOperation cropOp = new RasterCropOperation();
        GridCoverage2D cropedCoverage = cropOp.execute(inputDEM, cropShape);

        SimpleFeatureCollection result = null;
        if (cropedCoverage != null) {
            if (baseHeight == -9999.0) {
                DataStatistics statOp = new DataStatistics();
                StatisticsVisitorResult ret = statOp.getStatistics(cropedCoverage, 0);
                baseHeight = ret.getMean();
            }

            result = execute(cropedCoverage, baseHeight);
        }

        return result;
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputDEM, double baseHeight)
            throws ProcessException, IOException {
        NoData = RasterHelper.getNoDataValue(inputDEM);

        GridGeometry2D gridGeometry2D = inputDEM.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        CellSizeX = Math.abs(gridToWorld.getScaleX());
        CellSizeY = Math.abs(gridToWorld.getScaleY());
        Extent = new ReferencedEnvelope(inputDEM.getEnvelope());

        final int outputNoData = -9999;
        final double cellArea = CellSizeX * CellSizeY;
        CutFillResult result = new CutFillResult(baseHeight);

        PlanarImage inputImage = (PlanarImage) inputDEM.getRenderedImage();

        DiskMemImage outputImage = createDiskMemImage(inputDEM, RasterPixelType.SHORT);
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        readIter.startLines();
        writerIter.startLines();
        while (!readIter.finishedLines() && !writerIter.finishedLines()) {
            readIter.startPixels();
            writerIter.startPixels();

            while (!readIter.finishedPixels() && !writerIter.finishedPixels()) {
                final double gridVal = readIter.getSampleDouble(0);

                // Cut = 1, Fill = -1, Unchanged = 0
                int flag = outputNoData;

                if (!SSUtils.compareDouble(NoData, gridVal)) {
                    double diffVal = gridVal - baseHeight;
                    double volume = Math.abs(cellArea * diffVal);

                    if (diffVal > 0) {
                        result.cutArea += cellArea;
                        result.cutVolume += volume;
                        result.cutCount += 1;
                        flag = 1;
                    } else if (diffVal < 0) {
                        result.fillArea += cellArea;
                        result.fillVolume += volume;
                        result.fillCount += 1;
                        flag = -1;
                    } else {
                        result.unChangedArea += cellArea;
                        result.unChangedCount += 1;
                        flag = 0;
                    }
                }

                writerIter.setSample(0, flag);

                readIter.nextPixel();
                writerIter.nextPixel();
            }

            readIter.nextLine();
            writerIter.nextLine();
        }

        this.outputRaster = createGridCoverage("CutFill", outputImage, 0, outputNoData, -1, 1,
                Extent);

        // finally build features
        outputFeatures = this.buildFeatures(result);

        return outputFeatures;
    }

    private SimpleFeatureCollection buildFeatures(CutFillResult result)
            throws ProcessException, IOException {
        RasterToPolygonOperation converter = new RasterToPolygonOperation();
        SimpleFeatureCollection features = converter.execute(outputRaster, 0, false, "category");

        Map<Integer, List<Geometry>> map = buildMap(features);

        // create schema
        SimpleFeatureType schema = FeatureTypes.build(features.getSchema(), "CutFill");
        schema = FeatureTypes.add(schema, "count", Integer.class);
        schema = FeatureTypes.add(schema, "area", Double.class);
        schema = FeatureTypes.add(schema, "volume", Double.class);

        // create features
        ListFeatureCollection outputFc = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        int serialID = 0;
        for (Entry<Integer, List<Geometry>> entry : map.entrySet()) {
            Integer category = entry.getKey();
            List<Geometry> geometries = entry.getValue();
            if (geometries.size() == 0) {
                continue;
            }

            Geometry unionGeometry = null;
            try {
                CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
                unionGeometry = unionOp.union();
            } catch (IllegalStateException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }

            if (unionGeometry == null || unionGeometry.isEmpty()) {
                continue;
            }

            double area = result.getArea(category);
            double volume = result.getVolume(category);
            int count = result.getCount(category);

            String fid = schema.getTypeName() + "." + (++serialID);
            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(unionGeometry);

            newFeature.setAttribute("category", category);
            newFeature.setAttribute("count", count);
            newFeature.setAttribute("area", area);
            newFeature.setAttribute("volume", volume);

            outputFc.add(newFeature);
        }

        return outputFc;
    }

    private Map<Integer, List<Geometry>> buildMap(SimpleFeatureCollection features) {
        Map<Integer, List<Geometry>> map = new TreeMap<Integer, List<Geometry>>();
        map.put(Integer.valueOf(-1), new ArrayList<Geometry>());
        map.put(Integer.valueOf(0), new ArrayList<Geometry>());
        map.put(Integer.valueOf(1), new ArrayList<Geometry>());

        if (features == null) {
            return map;
        }

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Object val = feature.getAttribute("category");
                Integer category = Converters.convert(val, Integer.class);

                if (val != null) {
                    map.get(category).add((Geometry) feature.getDefaultGeometry());
                }
            }
        } finally {
            featureIter.close();
        }

        return map;
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