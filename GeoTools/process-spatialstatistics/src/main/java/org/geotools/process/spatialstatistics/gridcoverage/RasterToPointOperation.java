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
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Converts a raster dataset to point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference org.geotools.process.raster.PolygonExtractionProcess
 * 
 * @source $URL$
 */
public class RasterToPointOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterToPointOperation.class);

    final String VALUE_FIELD = "value";

    public SimpleFeatureCollection execute(GridCoverage2D inputGc, Integer bandIndex)
            throws IOException {
        return execute(inputGc, bandIndex, VALUE_FIELD);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputGc, Integer bandIndex,
            String valueField) throws IOException {
        return execute(inputGc, bandIndex, VALUE_FIELD, Boolean.FALSE);
    }

    public SimpleFeatureCollection execute(GridCoverage2D inputGc, Integer bandIndex,
            String valueField, Boolean retainNoData) throws IOException {
        CoordinateReferenceSystem crs = inputGc.getCoordinateReferenceSystem();
        String name = inputGc.getName().toString();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(name, Point.class, crs);

        RasterPixelType pixelType = RasterHelper.getTransferType(inputGc);
        switch (pixelType) {
        case BYTE:
        case SHORT:
        case INTEGER:
            featureType = FeatureTypes.add(featureType, valueField, Integer.class);
            break;
        case FLOAT:
        case DOUBLE:
            featureType = FeatureTypes.add(featureType, valueField, Double.class);
            break;
        }

        double noData = RasterHelper.getNoDataValue(inputGc);
        double cellSize = RasterHelper.getCellSize(inputGc);
        ReferencedEnvelope extent = new ReferencedEnvelope(inputGc.getEnvelope());
        GridTransformer trans = new GridTransformer(extent, cellSize);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        featureWriter.setFlushInterval(5000); // set flush interval

        PlanarImage inputImage = (PlanarImage) inputGc.getRenderedImage();
        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        try {
            int row = 0;
            inputIter.startLines();
            while (!inputIter.finishedLines()) {
                int column = 0;
                inputIter.startPixels();
                while (!inputIter.finishedPixels()) {
                    double curVal = inputIter.getSampleDouble(bandIndex);
                    if (retainNoData) {
                        Coordinate coord = trans.gridToWorldCoordinate(column, row);

                        // create feature and set geometry
                        SimpleFeature newFeature = featureWriter.buildFeature();
                        newFeature.setDefaultGeometry(gf.createPoint(coord));
                        newFeature.setAttribute(valueField, getPixelValue(curVal, pixelType));

                        featureWriter.write(newFeature);
                    } else {
                        if (!SSUtils.compareDouble(curVal, noData)) {
                            Coordinate coord = trans.gridToWorldCoordinate(column, row);

                            // create feature and set geometry
                            SimpleFeature newFeature = featureWriter.buildFeature();
                            newFeature.setDefaultGeometry(gf.createPoint(coord));
                            newFeature.setAttribute(valueField, getPixelValue(curVal, pixelType));

                            featureWriter.write(newFeature);
                        }
                    }

                    column++;
                    inputIter.nextPixel();
                }
                row++;
                inputIter.nextLine();
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(null);
        }

        return featureWriter.getFeatureCollection();
    }

    private Object getPixelValue(double curVal, RasterPixelType pixelType) {
        switch (pixelType) {
        case BYTE:
        case SHORT:
        case INTEGER:
            return (int) curVal;
        case FLOAT:
        case DOUBLE:
            return curVal;
        }

        return curVal;
    }
}
