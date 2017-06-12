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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.enumeration.ZonalStatisticsType;
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

    static final String AREA_FIELD = "Cell_Area";

    private double cellArea = 0.0;

    private ZonalStatisticsType statisticsType = ZonalStatisticsType.Mean; // default

    private String targetField = ZonalStatisticsType.Mean.name(); // default

    public RasterZonalOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection zoneFeatures,
            String targetField, GridCoverage2D valueCoverage, Integer bandIndex,
            ZonalStatisticsType statisticsType) throws IOException {
        this.statisticsType = statisticsType;
        this.targetField = targetField;

        // check crs
        CoordinateReferenceSystem sCRS = valueCoverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem tCRS = zoneFeatures.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(sCRS, tCRS)) {
            // reproject raster
            RasterReprojectOperation op = new RasterReprojectOperation();
            valueCoverage = op.execute(valueCoverage, tCRS, ResampleType.NEAREST);
        }

        // test intersection
        ReferencedEnvelope gridEnv = new ReferencedEnvelope(valueCoverage.getEnvelope());
        ReferencedEnvelope featureEnv = zoneFeatures.getBounds();
        com.vividsolutions.jts.geom.Envelope intEnv = featureEnv.intersection(gridEnv);
        if (intEnv == null || intEnv.isNull()) {
            // return empty result
            return insertFeatures(zoneFeatures, new Hashtable<Object, StatisticsVisitor>());
        }

        // crop coverage
        RasterCropOperation cropOp = new RasterCropOperation();
        GridCoverage2D cropGc = cropOp.execute(valueCoverage, new ReferencedEnvelope(intEnv, tCRS));

        // features to raster zone
        FeaturesToRasterOperation rsOp = new FeaturesToRasterOperation();
        rsOp.getRasterEnvironment().setCellSize(RasterHelper.getCellSize(valueCoverage));
        rsOp.getRasterEnvironment().setExtent(new ReferencedEnvelope(cropGc.getEnvelope2D(), tCRS));
        GridCoverage2D zonalGc = rsOp.execute(zoneFeatures);

        // calculate statistics
        Hashtable<Object, StatisticsVisitor> visitorMap = new Hashtable<Object, StatisticsVisitor>();

        final double inputNoData = RasterHelper.getNoDataValue(valueCoverage);
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
                final Integer key = zoneIter.getSample(0); // one band
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

                // Count, Sum, Mean, Minimum, Maximum, Range, StdDev, Area
                StatisticsVisitor visitor = visitorMap.get(featureID);
                if (visitor == null) {
                    if (statisticsType == ZonalStatisticsType.Count) {
                        newFeature.setAttribute(targetField, Integer.valueOf(0));
                    } else {
                        newFeature.setAttribute(targetField, null);
                    }
                    newFeature.setAttribute(AREA_FIELD, 0.0);
                } else {
                    StatisticsVisitorResult ret = visitor.getResult();
                    switch (statisticsType) {
                    case Count:
                        newFeature.setAttribute(targetField, ret.getCount());
                        break;
                    case Maximum:
                        newFeature.setAttribute(targetField, ret.getMaximum());
                        break;
                    case Mean:
                        newFeature.setAttribute(targetField, ret.getMean());
                        break;
                    case Minimum:
                        newFeature.setAttribute(targetField, ret.getMinimum());
                        break;
                    case Range:
                        newFeature.setAttribute(targetField, ret.getRange());
                        break;
                    case StdDev:
                        newFeature.setAttribute(targetField, ret.getStandardDeviation());
                        break;
                    case Sum:
                        newFeature.setAttribute(targetField, ret.getSum());
                        break;
                    default:
                        newFeature.setAttribute(targetField, ret.getMean());
                        break;
                    }
                    newFeature.setAttribute(AREA_FIELD, ret.getCount() * cellArea);
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

        if (statisticsType == ZonalStatisticsType.Count) {
            featureType = FeatureTypes.add(featureType, targetField, Integer.class, 10);
        } else {
            featureType = FeatureTypes.add(featureType, targetField, Double.class, 19);
        }

        // default
        featureType = FeatureTypes.add(featureType, AREA_FIELD, Double.class, 19);

        // prepare transactional feature store
        return getTransactionFeatureStore(featureType);
    }
}