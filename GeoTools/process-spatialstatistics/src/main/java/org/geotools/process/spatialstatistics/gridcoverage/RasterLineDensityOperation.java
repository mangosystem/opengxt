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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;

import org.eclipse.imagen.KernelImageN;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.iterator.RectIterFactory;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

/**
 * Calculates the density of linear features in the neighborhood of each output raster cell. <br>
 * Density is calculated in units of length per unit of area.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterLineDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterLineDensityOperation.class);

    public RasterLineDensityOperation() {

    }

    public GridCoverage2D execute(SimpleFeatureCollection lineFeatures, String weightField,
            double searchRadius) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(lineFeatures, Integer.MIN_VALUE);

        DiskMemImage outputImage = this.createDiskMemImage(gridExtent, RasterPixelType.FLOAT);
        WritableRaster raster = (WritableRaster) outputImage.getData();

        // step 1 : convert line to gridcoverage
        final PlanarImage sourceImage = lineToRaster(lineFeatures, weightField, searchRadius);

        // Density = ((L1 * V1) + (L2 * V2)) / (area_of_circle)
        final KernelImageN kernel = getKernel(searchRadius);

        // if unit is a meter, apply kilometers scale factor
        CoordinateReferenceSystem crs = lineFeatures.getSchema().getCoordinateReferenceSystem();
        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crs);
        if (!isGeographicCRS) {
            scaleArea = scaleArea / 1000.0;
        }

        final int imageWidth = outputImage.getWidth();
        final int imageHeight = outputImage.getHeight();

        final int xOrigin = kernel.getXOrigin();
        final int yOrigin = kernel.getYOrigin();
        final int w = kernel.getWidth();
        final int h = kernel.getHeight();

        RectIter readIter = RectIterFactory.create(sourceImage, sourceImage.getBounds());

        readIter.startLines();
        int row = 0;
        while (!readIter.finishedLines()) {
            readIter.startPixels();
            int col = 0;
            while (!readIter.finishedPixels()) {
                // (red << 16) + (green << 8) + (blue & 0xFF) + (alpha << 24);
                int[] data = readIter.getPixel(new int[4]);
                int rgba = (data[0] << 16) + (data[1] << 8) + (data[2] & 0xFF) + (data[3] << 24);

                double weight = Float.intBitsToFloat(rgba);
                if (weight == 0) {
                    readIter.nextPixel();
                    col++;
                    continue;
                }

                // raster index
                int x = col - xOrigin;
                int y = row - yOrigin;
                int xw = w;
                int yh = h;

                // kernel index
                int startCol = 0;
                int startRow = 0;
                int endCol = w;
                int endRow = h;

                if (x < 0 || y < 0) {
                    if (x < 0) {
                        xw = x + xw;
                        startCol = Math.abs(x);
                        x = 0;
                    } else if (y < 0) {
                        yh = y + yh;
                        startRow = Math.abs(y);
                        y = 0;
                    }
                }

                if ((x + xw) > imageWidth || (y + yh) > imageHeight) {
                    if ((x + xw) > imageWidth) {
                        int dif = (x + xw) - imageWidth;
                        xw = xw - dif;
                        endCol = xw;
                    } else if ((y + yh) > imageHeight) {
                        int dif = (y + yh) - imageHeight;
                        yh = yh - dif;
                        endRow = yh;
                    }
                }

                if (x < 0 || y < 0 || xw < 1 || yh < 1) {
                    continue;
                }

                // get data
                float[] samples = raster.getSamples(x, y, xw, yh, 0, new float[xw * yh]);

                int index = 0;
                for (int irow = startRow; irow < endRow; irow++) {
                    for (int icol = startCol; icol < endCol; icol++) {
                        double kernelValue = kernel.getElement(icol, irow);
                        if (kernelValue == 0) {
                            index++;
                            continue;
                        }

                        double wValue = ((weight * kernelValue) / scaleArea) + samples[index];

                        samples[index] = (float) wValue;
                        this.maxValue = Math.max(maxValue, wValue);
                        index++;
                    }
                }

                // set data
                raster.setSamples(x, y, xw, yh, 0, samples);

                readIter.nextPixel();
                col++;
            }
            readIter.nextLine();
            row++;
        }

        // finally, set raster data to image
        outputImage.setData(raster);

        return createGridCoverage("LineDensity", outputImage);
    }

    protected PlanarImage lineToRaster(SimpleFeatureCollection lineFeatures, String weightField,
            double searchRadius) {
        DiskMemImage dmImage = null;

        ColorModel colorModel = ColorModel.getRGBdefault();
        SampleModel smpModel = colorModel.createCompatibleSampleModel(64, 64);
        Dimension dim = RasterHelper.getDimension(gridExtent, pixelSizeX, pixelSizeY);

        dmImage = new DiskMemImage(0, 0, dim.width, dim.height, 0, 0, smpModel, colorModel);
        dmImage.setUseCommonCache(true);

        // create graphics
        Graphics2D g2D = dmImage.createGraphics();
        g2D.setPaintMode();
        g2D.setComposite(AlphaComposite.Src);

        // set background value to zero
        g2D.setPaint(new Color(Float.floatToIntBits(0.0f), true));
        g2D.fillRect(0, 0, dmImage.getWidth(), dmImage.getHeight());
        g2D.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f));

        g2D.setComposite(BlendAddComposite.getInstance());

        // setup affine transform
        double x_scale = dmImage.getWidth() / gridExtent.getWidth();
        double y_scale = dmImage.getHeight() / gridExtent.getHeight();
        Coordinate centerPos = gridExtent.centre();

        AffineTransform affineTrans = new AffineTransform();
        affineTrans.translate(dmImage.getWidth() / 2.0, dmImage.getHeight() / 2.0);
        affineTrans.scale(x_scale, -y_scale);
        affineTrans.translate(-centerPos.x, -centerPos.y);

        // draw line
        Filter filter = getBBoxFilter(lineFeatures.getSchema(), gridExtent, searchRadius);

        SimpleFeatureIterator featureIter = lineFeatures.subCollection(filter).features();
        try {
            Expression valueExp = ff.literal(1.0); // default
            if (!StringHelper.isNullOrEmpty(weightField)) {
                valueExp = ff.property(weightField);
            }

            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Number gridValue = valueExp.evaluate(feature, Double.class);
                if (gridValue == null) {
                    continue; // skip line
                }

                int intBits = Float.floatToIntBits(gridValue.floatValue());
                g2D.setPaint(new Color(intBits, true));

                GeneralPath path = new GeneralPath();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {
                    LineString lineString = (LineString) geometry.getGeometryN(i);

                    path.reset();
                    addLineStringToPath(false, lineString, path);
                    path.transform(affineTrans);

                    g2D.draw(path);
                }
            }
        } finally {
            featureIter.close();
        }

        g2D.dispose();

        return dmImage;
    }

    private void addLineStringToPath(boolean isRing, LineString lineString,
            GeneralPath targetPath) {
        Coordinate[] cs = lineString.getCoordinates();

        double xOffset = 0;
        double yOffset = 0;

        for (int i = 0; i < cs.length; i++) {
            if (i == 0) {
                targetPath.moveTo(cs[i].x - xOffset, cs[i].y + yOffset);
            } else {
                targetPath.lineTo(cs[i].x - xOffset, cs[i].y + yOffset);
            }
        }

        if (isRing && !lineString.isClosed()) {
            targetPath.closePath();
        }
    }

    private KernelImageN getKernel(double searchRadius) {
        scaleArea = 0.0;

        // convert map unit to cell unit
        double cellSize = Math.max(pixelSizeX, pixelSizeY);
        int radius = (int) Math.floor(searchRadius / cellSize);

        // Creates a circular kernel with width 2*radius + 1
        if (radius < 1) {
            radius = 1;
        }

        final int size = 2 * radius + 1;
        final float[] data = new float[size * size];
        final double r2 = radius * radius;
        final double cellArea = pixelSizeX * pixelSizeY;
        int valid = 0;

        for (int y = -radius; y <= radius; y++) {
            final double yy = y * y;
            for (int x = -radius; x <= radius; x++) {
                final int idx = (y + radius) * size + (x + radius);
                final double dist2 = (x * x) + yy;
                if (dist2 <= r2) {
                    data[idx] = (float) cellSize;
                    scaleArea += cellArea;
                    valid++;
                } else {
                    data[idx] = 0.0f;
                }
            }
        }

        KernelImageN kernel = new KernelImageN(size, size, radius, radius, data);

        this.minValue = 0.0;
        this.maxValue = maxValue * valid;

        return kernel;
    }

    static final class BlendAddComposite implements Composite {

        private final float alpha;

        private BlendAddComposite() {
            this(1.0f);
        }

        private BlendAddComposite(float alpha) {
            this.alpha = Math.max(Math.min(alpha, 1.0f), 0.0f);
        }

        public static BlendAddComposite getInstance() {
            return new BlendAddComposite();
        }

        @Override
        public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
                RenderingHints hints) {

            return new CompositeContext() {
                @Override
                public void dispose() {

                }

                @Override
                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int width = Math.min(src.getWidth(), dstIn.getWidth());
                    int height = Math.min(src.getHeight(), dstIn.getHeight());

                    int[] srcPixels = new int[width];
                    int[] dstPixels = new int[width];

                    for (int y = 0; y < height; y++) {
                        src.getDataElements(0, y, width, 1, srcPixels);
                        dstIn.getDataElements(0, y, width, 1, dstPixels);
                        for (int x = 0; x < width; x++) {
                            float srcVal = Float.intBitsToFloat(srcPixels[x]);
                            float dstVal = Float.intBitsToFloat(dstPixels[x]);

                            int pixel = dstPixels[x];
                            int dr = (pixel >> 16) & 0xFF;
                            int dg = (pixel >> 8) & 0xFF;
                            int db = (pixel) & 0xFF;
                            int da = (pixel >> 24) & 0xFF;

                            // Add
                            pixel = Float.floatToIntBits(srcVal + dstVal);
                            int or = (pixel >> 16) & 0xFF;
                            int og = (pixel >> 8) & 0xFF;
                            int ob = (pixel) & 0xFF;
                            int oa = (pixel >> 24) & 0xFF;

                            // mixes the result with the alpha blending
                            or = (int) (dr + (or - dr) * alpha);
                            og = (int) (dg + (og - dg) * alpha);
                            ob = (int) (db + (ob - db) * alpha);
                            oa = (int) (da + (oa - da) * alpha);

                            dstPixels[x] = oa << 24 | or << 16 | og << 8 | ob & 0xFF;
                        }
                        dstOut.setDataElements(0, y, width, 1, dstPixels);
                    }
                }
            };
        }
    }
}
