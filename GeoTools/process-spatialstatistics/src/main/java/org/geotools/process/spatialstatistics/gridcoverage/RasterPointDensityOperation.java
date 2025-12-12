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

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.KernelImageN;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterNeighborhood.NeighborUnits;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Calculates a magnitude per unit area from point features that fall within a neighborhood around each cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterPointDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterPointDensityOperation.class);

    // Silverman, B. W. Density Estimation for Statistics and Data Analysis. New York: Chapman and Hall, 1986.

    // default = circle, 8 * 8 cell unit
    RasterNeighborhood rnh = new RasterNeighborhood();

    public RasterPointDensityOperation() {
        // default setting
        rnh.setCircle(8.0, NeighborUnits.CELL);
    }

    public void setNeighbor(RasterNeighborhood neighborhood) {
        this.rnh = neighborhood;
    }

    public RasterNeighborhood getNeighbor() {
        return rnh;
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Integer.MIN_VALUE);

        DiskMemImage outputImage = this.createDiskMemImage(gridExtent, RasterPixelType.FLOAT);
        WritableRaster raster = (WritableRaster) outputImage.getData();

        final KernelImageN kernel = getKernel(this.rnh);

        // if unit is a meter, apply kilometers scale factor
        CoordinateReferenceSystem crs = pointFeatures.getSchema().getCoordinateReferenceSystem();
        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crs);
        if (!isGeographicCRS) {
            scaleArea = scaleArea / 1000000.0;
        }

        double cellSize = Math.max(pixelSizeX, pixelSizeY);
        double searchRadius = Math.max(kernel.getWidth(), kernel.getHeight()) * cellSize;
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

        return createGridCoverage("PointDensity", scaleUnit(outputImage));
    }

    public GridCoverage2D execute_org(SimpleFeatureCollection pointFeatures,
            String populationField) {
        // Legacy JAI-based path replaced by the manual ImageN implementation
        return execute(pointFeatures, populationField);
    }

    private KernelImageN getKernel(RasterNeighborhood rnh) {
        scaleArea = 0.0;

        KernelImageN kernel = null;

        switch (rnh.getNeighborType()) {
        case CIRCLE:
            {
                int radius = (int) rnh.getRadius();
                if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                    // convert map unit to cell unit
                    double cellSize = Math.max(pixelSizeX, pixelSizeY);
                    radius = (int) Math.floor(rnh.getRadius() / cellSize);
                }

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
                            data[idx] = 1.0f;
                            scaleArea += cellArea;
                            valid++;
                        } else {
                            data[idx] = 0.0f;
                        }
                    }
                }
                kernel = new KernelImageN(size, size, radius, radius, data);
                this.minValue = 0.0;
                this.maxValue = maxValue * valid;
                break;
            }
        case RECTANGLE:
            {
                int rw = (int) rnh.getWidth();
                int rh = (int) rnh.getHeight();
                if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                    // convert map unit to cell unit
                    rw = (int) Math.floor(rnh.getWidth() / pixelSizeX);
                    rh = (int) Math.floor(rnh.getHeight() / pixelSizeY);
                }
                if (rw < 1) {
                    rw = 1;
                }
                if (rh < 1) {
                    rh = 1;
                }

                final int width = 2 * rw + 1;
                final int height = 2 * rh + 1;
                final float[] data = new float[width * height];
                final double cellArea = pixelSizeX * pixelSizeY;
                int valid = 0;

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        data[y * width + x] = 1.0f;
                        scaleArea += cellArea;
                        valid++;
                    }
                }
                kernel = new KernelImageN(width, height, rw, rh, data);
                this.minValue = 0.0;
                this.maxValue = maxValue * valid;
                break;
            }
        }

        return kernel;
    }
}
