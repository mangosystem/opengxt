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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.imagen.KernelImageN;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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

        KernelImageN kernel = getKernel(searchRadius);

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
                    try {
                        float[] samples = raster.getSamples(x, y, xw, yh, 0, new float[xw * yh]);

                        int index = 0;
                        for (int row = startRow; row < endRow; row++) {
                            for (int col = startCol; col < endCol; col++) {
                                double kernelValue = kernel.getElement(col, row);
                                if (kernelValue == 0) {
                                    index++;
                                    continue;
                                }

                                double wValue = ((weight * kernelValue) / scaleArea)
                                        + samples[index];

                                samples[index] = (float) wValue;
                                this.maxValue = Math.max(maxValue, wValue);
                                index++;
                            }
                        }

                        // set data
                        raster.setSamples(x, y, xw, yh, 0, samples);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINEST, e.getMessage());
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        // finally, set raster data to image
        outputImage.setData(raster);

        return createGridCoverage("KernelDensity", outputImage);
    }

    private KernelImageN getKernel(double searchRadius) {
        scaleArea = 0.0;

        // http://en.wikipedia.org/wiki/Kernel_(statistics)
        double cellSize = Math.max(pixelSizeX, pixelSizeY);
        int radius = (int) Math.floor(searchRadius / cellSize);
        int width = 2 * radius + 1;
        final double r2 = radius * radius;

        // calculate area
        final double cellArea = pixelSizeX * pixelSizeY;

        float[] weights = new float[width * width];
        int valid = 0;

        for (int dY = -radius; dY <= radius; dY++) {
            final double dy2 = dY * dY;
            for (int dX = -radius; dX <= radius; dX++) {
                final int index = ((dY + radius) * width) + (dX + radius);
                final double dist2 = (dX * dX) + dy2;
                if (dist2 > r2) {
                    weights[index] = 0.0f;
                    continue;
                }

                double dist = Math.sqrt(dist2);
                double u = radius == 0 ? 0 : dist / radius;
                double value = 0.0;

                switch (this.kernelType) {
                case Binary:
                    value = 1.0;
                    break;
                case Cosine:
                    value = (Math.PI / 4.0) * Math.cos((Math.PI * u) / 2.0);
                    break;
                case Distance:
                    value = dist;
                    break;
                case Epanechnikov:
                    value = 3.0 * (1.0 - (u * u)) / 4.0;
                    break;
                case Gaussian:
                    value = (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5 * u * u);
                    break;
                case InverseDistance:
                    value = dist == 0 ? 0.0 : 1.0 / dist;
                    break;
                case Quadratic:
                    // Silverman(1986, p. 76, equation 4.5).
                    double termq = 1.0 - (dist2 / r2);
                    value = 3.0 * termq * termq;
                    break;
                case Quartic:
                    value = (15.0 / 16.0) * Math.pow(1.0 - u * u, 2.0);
                    break;
                case Triangular:
                    value = 1.0 - u;
                    break;
                case Triweight:
                    value = (35.0 / 32.0) * Math.pow(1.0 - u * u, 3.0);
                    break;
                case Tricube:
                    final double cTricube = 70.0 / 81.0;
                    double term = 1.0 - Math.pow(Math.abs(u), 3.0);
                    value = cTricube * term * term * term;
                    break;
                }

                if (value < 0) {
                    value = 0;
                }

                weights[index] = (float) value;
                if (value != 0.0) {
                    scaleArea += cellArea;
                    valid++;
                }
            }
        }

        KernelImageN kernel = new KernelImageN(width, width, radius, radius, weights);

        this.minValue = 0.0;
        this.maxValue = maxValue * valid;

        return kernel;
    }
}
