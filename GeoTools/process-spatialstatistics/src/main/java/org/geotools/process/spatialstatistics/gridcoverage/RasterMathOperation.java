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

import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Performs mathematical operations on raster using expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterMathOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterMathOperation.class);

    GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public GridCoverage2D execute(GridCoverage2D inputGc, Integer bandIndex, Expression expression) {

        // prepare feature
        SimpleFeature feature = super.createTemplateFeature(inputGc);

        // create image
        RasterPixelType pixelType = RasterPixelType.DOUBLE;
        DiskMemImage outputImage = this.createDiskMemImage(inputGc, pixelType);

        PlanarImage inputImage = (PlanarImage) inputGc.getRenderedImage();
        this.NoData = RasterHelper.getNoDataValue(inputGc);
        double cellSize = RasterHelper.getCellSize(inputGc);

        ReferencedEnvelope extent = new ReferencedEnvelope(inputGc.getEnvelope());
        GridTransformer trans = new GridTransformer(extent, cellSize);

        RectIter inputIter = RectIterFactory.create(inputImage, inputImage.getBounds());
        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        int row = 0;
        inputIter.startLines();
        writerIter.startLines();
        while (!inputIter.finishedLines() && !writerIter.finishedLines()) {
            int column = 0;
            inputIter.startPixels();
            writerIter.startPixels();
            while (!inputIter.finishedPixels() && !writerIter.finishedPixels()) {
                final double gridVal = inputIter.getSampleDouble(bandIndex);

                if (SSUtils.compareDouble(gridVal, this.NoData)) {
                    writerIter.setSample(0, this.NoData);
                } else {
                    Coordinate coord = trans.gridToWorldCoordinate(column, row);
                    feature.setDefaultGeometry(gf.createPoint(coord));
                    feature.setAttribute(1, gridVal); // raster name
                    feature.setAttribute(2, gridVal); // Value

                    Double value = expression.evaluate(feature, Double.class);
                    if (value == null || value.isInfinite() || value.isNaN()) {
                        writerIter.setSample(0, this.NoData);
                    } else {
                        writerIter.setSample(0, value.doubleValue());
                        updateStatistics(value);
                    }
                }

                column++;
                inputIter.nextPixel();
                writerIter.nextPixel();
            }
            row++;
            inputIter.nextLine();
            writerIter.nextLine();
        }

        return createGridCoverage(inputGc.getName(), outputImage);
    }
}
