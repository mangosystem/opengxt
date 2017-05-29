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
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Calculates statistics on values of a raster within the zones of another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterZonalOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterZonalOperation.class);

    final String[] FIELDS = { "Count", "Area", "Min", "Max", "Range", "Mean", "StdDev", "Sum" };

    private double cellArea = 0.0;

    public RasterZonalOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection zoneFeatures,
            GridCoverage2D valueCoverage, Integer bandIndex) throws IOException {
        // check crs
        CoordinateReferenceSystem gCRS = valueCoverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem fCRS = zoneFeatures.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(gCRS, fCRS)) {
            throw new ProcessException("zoneFeatures's CRS must be the same as valueCoverage CRS!");
        }

        // test intersection
        ReferencedEnvelope gridEnv = new ReferencedEnvelope(valueCoverage.getEnvelope());
        ReferencedEnvelope featureEnv = new ReferencedEnvelope(zoneFeatures.getBounds(), gCRS);
        com.vividsolutions.jts.geom.Envelope jtsEnvelope = featureEnv.intersection(gridEnv);
        if (jtsEnvelope == null || jtsEnvelope.isNull()) {
            // return empty result
            return insertFeatures(zoneFeatures, new Hashtable<Object, StatisticsVisitor>());
        }

        // crop coverage
        final double inputNoData = RasterHelper.getNoDataValue(valueCoverage);
        ReferencedEnvelope newExtent = new ReferencedEnvelope(jtsEnvelope, gCRS);

        RasterCropOperation cropOp = new RasterCropOperation();
        GridCoverage2D cropGc = cropOp.execute(valueCoverage, newExtent);

        // features to raster zone
        newExtent = new ReferencedEnvelope(cropGc.getEnvelope2D(), gCRS);
        FeaturesToRasterOperation rsOp = new FeaturesToRasterOperation();
        rsOp.getRasterEnvironment().setCellSize(RasterHelper.getCellSize(valueCoverage));
        rsOp.getRasterEnvironment().setExtent(newExtent);
        GridCoverage2D zonalGc = rsOp.execute(zoneFeatures);

        // calculate statistics
        Hashtable<Object, StatisticsVisitor> visitorMap = new Hashtable<Object, StatisticsVisitor>();

        final double zoneNoData = RasterHelper.getNoDataValue(zonalGc);
        final double cellSize = RasterHelper.getCellSize(zonalGc);
        cellArea = cellSize * cellSize;

        PlanarImage zoneImage = (PlanarImage) zonalGc.getRenderedImage();
        PlanarImage inputImage = (PlanarImage) cropGc.getRenderedImage();

        RectIter zoneIter = RectIterFactory.create(zoneImage, zoneImage.getBounds());
        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());
        zoneIter.startLines();
        inputIter.startLines();
        while (!zoneIter.finishedLines() && !inputIter.finishedLines()) {
            zoneIter.startPixels();
            inputIter.startPixels();
            while (!zoneIter.finishedPixels() && !inputIter.finishedPixels()) {
                final double key = zoneIter.getSampleDouble(0); // one band
                final double value = inputIter.getSampleDouble(bandIndex);

                if (!SSUtils.compareDouble(zoneNoData, key)
                        && !SSUtils.compareDouble(inputNoData, value)) {
                    StatisticsVisitor visitor = visitorMap.get(key);
                    if (visitor == null) {
                        visitor = new StatisticsVisitor(new DoubleStrategy());
                        visitorMap.put(key, visitor);
                    }
                    visitor.visit(value);
                }

                zoneIter.nextPixel();
                inputIter.nextPixel();
            }
            zoneIter.nextLine();
            inputIter.nextLine();
        }

        cropGc.dispose(false);
        zonalGc.dispose(false);

        // build result
        return insertFeatures(zoneFeatures, visitorMap);
    }

    private SimpleFeatureCollection insertFeatures(SimpleFeatureCollection zoneFeatures,
            Hashtable<Object, StatisticsVisitor> visitorMap) throws IOException {

        // prepare transactional feature store
        IFeatureInserter featureWriter = prepareFeatureWriter(zoneFeatures);

        // insert features
        SimpleFeatureIterator featureIter = zoneFeatures.features();
        try {
            int featureID = 1;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                StatisticsVisitor visitor = visitorMap.get(featureID);
                if (visitor == null) {
                    newFeature.setAttribute(FIELDS[0], 0); // Count
                    newFeature.setAttribute(FIELDS[1], null); // Min
                    newFeature.setAttribute(FIELDS[2], null); // Max
                    newFeature.setAttribute(FIELDS[3], null); // Range
                    newFeature.setAttribute(FIELDS[4], null); // Mean
                    newFeature.setAttribute(FIELDS[5], null); // StdDev
                    newFeature.setAttribute(FIELDS[6], 0.0); // Sum
                    newFeature.setAttribute(FIELDS[7], 0.0); // Area
                } else {
                    StatisticsVisitorResult ret = visitor.getResult();
                    newFeature.setAttribute("Count", ret.getCount());
                    newFeature.setAttribute("Min", ret.getMinimum());
                    newFeature.setAttribute("Max", ret.getMaximum());
                    newFeature.setAttribute("Range", ret.getRange());
                    newFeature.setAttribute("Mean", ret.getMean());
                    newFeature.setAttribute("Std", ret.getStandardDeviation());
                    newFeature.setAttribute("Sum", ret.getSum());
                    newFeature.setAttribute("Area", ret.getCount() * cellArea);
                }
                featureWriter.write(newFeature);
                featureID++;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private IFeatureInserter prepareFeatureWriter(SimpleFeatureCollection zoneSfs) {
        SimpleFeatureType featureType = zoneSfs.getSchema();
        featureType = FeatureTypes.add(featureType, FIELDS[0], Integer.class, 38);
        for (int index = 1; index < FIELDS.length; index++) {
            featureType = FeatureTypes.add(featureType, FIELDS[index], Integer.class, 38);
        }

        // prepare transactional feature store
        return getTransactionFeatureStore(featureType);
    }
}