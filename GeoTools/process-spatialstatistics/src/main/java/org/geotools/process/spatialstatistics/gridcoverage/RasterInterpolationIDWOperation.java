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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Interpolates a raster surface from points using an inverse distance weighted (IDW) technique.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterInterpolationIDWOperation extends RasterInterpolationOperator {
    protected static final Logger LOGGER = Logging.getLogger(RasterInterpolationIDWOperation.class);

    /**
     * Interpolates a raster surface from points using an inverse distance weighted (IDW) technique.
     * 
     * @param pointFeatures The input point features containing the z-values to be interpolated into a surface raster.
     * @param valueField The field that holds a height or magnitude value for each point.
     * @return The output interpolated surface raster.
     */
    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String valueField) {
        return execute(pointFeatures, valueField, 2.0d, new RasterRadius());
    }

    /**
     * Interpolates a raster surface from points using an inverse distance weighted (IDW) technique.
     * 
     * @param pointFeatures The input point features containing the z-values to be interpolated into a surface raster.
     * @param valueField The field that holds a height or magnitude value for each point.
     * @param power The exponent of distance. the most reasonable results will be obtained using values from 0.5 to 3. The default is 2.
     * @param rasterRadius defines which of the input points will be used to interpolate the value for each cell in the output raster.
     * @return The output interpolated surface raster.
     */
    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String valueField,
            double power, RasterRadius rasterRadius) {
        valueField = FeatureTypes.validateProperty(pointFeatures.getSchema(), valueField);
        if (pointFeatures.getSchema().indexOf(valueField) == -1) {
            LOGGER.log(Level.FINER, valueField + " does not exist!");
            return null;
        }

        // default = Variable, 12, Double.NaN
        RasterRadius radius = rasterRadius == null ? new RasterRadius() : rasterRadius;

        // calculate extent & cellsize
        RasterPixelType pixelType = RasterPixelType.FLOAT;
        calculateExtentAndCellSize(pointFeatures, RasterHelper.getDefaultNoDataValue(pixelType));

        // extract the input observation points
        Coordinate[] pts = extractPoints(pointFeatures, valueField);
        final IDWInterpolator interpolator = new IDWInterpolator(pts, radius, power);

        // create image & write pixels
        final DiskMemImage oi = createDiskMemImage(Extent, pixelType);
        final GridTransformer trans = new GridTransformer(Extent, CellSize);

        // divide 500 pixels
        final java.awt.Rectangle bounds = oi.getBounds();
        final int count = Math.max(bounds.width, bounds.height) / 500;
        if (count == 0) {
            WritableRectIter writer = RectIterFactory.createWritable(oi, bounds);

            int y = 0;
            writer.startLines();
            while (!writer.finishedLines()) {
                writer.startPixels();
                int x = 0;
                while (!writer.finishedPixels()) {
                    final Coordinate realPos = trans.gridToWorldCoordinate(x, y);
                    final double retVal = interpolator.getValue(realPos);
                    writer.setSample(0, retVal);
                    updateStatistics(retVal);
                    writer.nextPixel();
                    x++;
                }
                writer.nextLine();
                y++;
            }
        } else {
            final int sizeX = bounds.width / count;
            final int sizeY = bounds.height / count;

            List<Thread> threads = new ArrayList<Thread>();
            for (int x = 0; x < count; x++) {
                int posX = x == 0 ? x : (x * sizeX) + x;
                for (int y = 0; y < count; y++) {
                    int posY = y == 0 ? y : (y * sizeY) + y;
                    java.awt.Rectangle rect = new Rectangle(posX, posY, sizeX + 1, sizeY + 1);
                    Thread thread = new Thread(new PartialInterpolator(oi, rect, interpolator,
                            trans));
                    thread.start();
                    threads.add(thread);
                }
            }

            for (int i = 0; i < threads.size(); i++) {
                try {
                    threads.get(i).join();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }

        return createGridCoverage("IDW", oi);
    }

    final class PartialInterpolator implements Runnable {
        private DiskMemImage oi;

        private java.awt.Rectangle rect;

        private AbstractInterpolator interpolator;

        private GridTransformer trans;

        public PartialInterpolator(DiskMemImage oi, Rectangle rect,
                AbstractInterpolator interpolator, GridTransformer trans) {
            this.oi = oi;
            this.rect = rect;
            this.interpolator = interpolator;
            this.trans = trans;
        }

        public void run() {
            WritableRectIter writer = RectIterFactory.createWritable(oi, rect);

            writer.startLines();
            int y = rect.y;
            while (!writer.finishedLines()) {
                writer.startPixels();
                int x = rect.x;
                while (!writer.finishedPixels()) {
                    final Coordinate realPos = trans.gridToWorldCoordinate(x, y);
                    final double retVal = interpolator.getValue(realPos);
                    writer.setSample(0, retVal);
                    updateStatistics(retVal);
                    writer.nextPixel();
                    x++;
                }
                writer.nextLine();
                y++;
            }
        }
    }
}
