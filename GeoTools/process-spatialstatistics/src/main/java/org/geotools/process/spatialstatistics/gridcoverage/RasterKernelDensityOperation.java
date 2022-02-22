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

import java.awt.image.WritableRaster;
import java.util.logging.Logger;

import javax.media.jai.KernelJAI;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Calculates a magnitude per unit area from point features using a kernel function to fit a smoothly tapered surface to each point.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterKernelDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterKernelDensityOperation.class);

    private KernelType kernelType = KernelType.Quadratic;

    public KernelType getKernelType() {
        return kernelType;
    }

    public void setKernelType(KernelType kernelType) {
        this.kernelType = kernelType;
    }

    public RasterKernelDensityOperation() {

    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField) {
        // The default is the shortest of the width or height of the extent of in_features
        // in the output spatial reference, divided by 30
        ReferencedEnvelope extent = pointFeatures.getBounds();
        double searchRadius = Math.min(extent.getWidth(), extent.getHeight()) / 30.0;
        return execute(pointFeatures, weightField, searchRadius);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField,
            double searchRadius) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Integer.MIN_VALUE);

        DiskMemImage outputImage = this.createDiskMemImage(gridExtent, RasterPixelType.FLOAT);
        WritableRaster raster = (WritableRaster) outputImage.getData();

        KernelJAI kernel = getKernel(searchRadius);

        // if unit is a meter, apply kilometers scale factor
        CoordinateReferenceSystem crs = pointFeatures.getSchema().getCoordinateReferenceSystem();
        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crs);
        if (!isGeographicCRS) {
            scaleArea = scaleArea / 1000000.0;
        }

        Filter filter = getBBoxFilter(pointFeatures.getSchema(), gridExtent, searchRadius);

        GridTransformer trans = new GridTransformer(gridExtent, pixelSizeX, pixelSizeY);
        SimpleFeatureIterator featureIter = pointFeatures.subCollection(filter).features();
        try {
            Expression weightExp = ff.literal(1.0); // default
            if (!StringHelper.isNullOrEmpty(weightField)) {
                weightExp = ff.property(weightField);
            }

            final int imageWidth = outputImage.getWidth();
            final int imageHeight = outputImage.getHeight();

            final int xOrigin = kernel.getXOrigin();
            final int yOrigin = kernel.getYOrigin();
            final int w = kernel.getWidth();
            final int h = kernel.getHeight();

            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                if (multiPoint == null || multiPoint.isEmpty()) {
                    continue;
                }

                Double dblVal = weightExp.evaluate(feature, Double.class);
                final double weight = dblVal == null ? 1.0 : dblVal.doubleValue();
                if (weight == 0) {
                    continue;
                }

                // Multipoints are treated as a set of individual points.
                Coordinate[] coordinates = multiPoint.getCoordinates();
                for (int part = 0; part < coordinates.length; part++) {
                    Coordinate realPos = coordinates[part];
                    final GridCoordinates2D gridPos = trans.worldToGrid(realPos);

                    // raster index
                    int x = gridPos.x - xOrigin;
                    int y = gridPos.y - yOrigin;
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
                    for (int row = startRow; row < endRow; row++) {
                        for (int col = startCol; col < endCol; col++) {
                            double kernelValue = kernel.getElement(col, row);
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
                }
            }
        } finally {
            featureIter.close();
        }

        // finally, set raster data to image
        outputImage.setData(raster);

        return createGridCoverage("KernelDensity", outputImage);
    }

    private KernelJAI getKernel(double searchRadius) {
        scaleArea = 0.0;

        // http://en.wikipedia.org/wiki/Kernel_(statistics)
        double cellSize = Math.max(pixelSizeX, pixelSizeY);
        int radius = (int) Math.floor(searchRadius / cellSize);
        int width = 2 * radius + 1;
        final double r2 = radius * radius;

        // calculate area
        final double cellArea = pixelSizeX * pixelSizeY;

        // build kernel
        final KernelJAI binKernel = KernelFactory.createCircle(radius, ValueType.BINARY);

        // use cell's area
        final float[] data = binKernel.getKernelData();
        int valid = 0;
        for (int index = 0; index < data.length; index++) {
            if (data[index] != 0.0) {
                scaleArea += cellArea;
                valid++;
            }
        }

        KernelJAI kernel = null;
        switch (this.kernelType) {
        case Binary:
            kernel = KernelFactory.createCircle(radius, ValueType.BINARY);
            break;
        case Cosine:
            kernel = KernelFactory.createCircle(radius, ValueType.COSINE);
            break;
        case Distance:
            kernel = KernelFactory.createCircle(radius, ValueType.DISTANCE);
            break;
        case Epanechnikov:
            kernel = KernelFactory.createCircle(radius, ValueType.EPANECHNIKOV);
            break;
        case Gaussian:
            kernel = KernelFactory.createCircle(radius, ValueType.GAUSSIAN);
            break;
        case InverseDistance:
            kernel = KernelFactory.createCircle(radius, ValueType.INVERSE_DISTANCE);
            break;
        case Quadratic:
            float[] weights = new float[width * width];
            for (int dY = -radius; dY <= radius; dY++) {
                final double dy2 = dY * dY;
                for (int dX = -radius; dX <= radius; dX++) {
                    final int index = ((dY + radius) * width) + (dX + radius);
                    if (data[index] == 0.0) {
                        weights[index] = 0;
                    } else {
                        // Silverman(1986, p. 76, equation 4.5).
                        final double dxdy = (dX * dX) + dy2;
                        final double termq = 1.0 - (dxdy / r2);
                        final double kde = 3.0 * termq * termq;

                        weights[index] = (float) kde;
                    }
                }
            }
            kernel = new KernelJAI(width, width, weights);
            break;
        case Quartic:
            kernel = KernelFactory.createCircle(radius, ValueType.QUARTIC);
            break;
        case Triangular:
            kernel = KernelFactory.createCircle(radius, ValueType.TRIANGULAR);
            break;
        case Triweight:
            kernel = KernelFactory.createCircle(radius, ValueType.TRIWEIGHT);
            break;
        case Tricube:
            // http://en.wikipedia.org/wiki/Kernel_(statistics)
            final double C_TRICUBE = 70.0 / 81.0;
            float[] tcWeights = new float[width * width];
            for (int dY = -radius; dY <= radius; dY++) {
                final double dy2 = dY * dY;
                for (int dX = -radius; dX <= radius; dX++) {
                    final int index = ((dY + radius) * width) + (dX + radius);
                    if (data[index] == 0.0) {
                        tcWeights[index] = 0;
                    } else {
                        final double dxdy = (dX * dX) + dy2;
                        final double u = Math.abs(Math.sqrt(dxdy) / radius);

                        final double termq = 1.0 - (u * u * u);
                        final double kde = C_TRICUBE * termq * termq * termq;

                        tcWeights[index] = (float) kde;
                    }
                }
            }
            kernel = new KernelJAI(width, width, tcWeights);
            break;
        }

        this.minValue = 0.0;
        this.maxValue = maxValue * valid;

        return kernel;
    }
}
