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

import java.awt.RenderingHints;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterNeighborhood.NeighborUnits;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import si.uom.SI;

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

        final KernelJAI kernel = getKernel(this.rnh);

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
        // step 1 : convert point to gridcoverage : Sum
        final PlanarImage outputImage = pointToRaster(pointFeatures, populationField);

        // step 2 Only a circular neighborhood is possible
        final KernelJAI kernel = getKernel(this.rnh);

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        final ParameterBlockJAI pb = new ParameterBlockJAI("Convolve");
        pb.setSource("source0", outputImage);
        pb.setParameter("kernel", kernel);

        PlanarImage densityImage = JAI.create("Convolve", pb, hints);

        // If an area unit is selected, the calculated density for the cell is multiplied by the
        // appropriate factor before it is written to the output raster.
        // For example, if the input units are meters, the output area units will default to square
        // kilometers. Comparing a unit scale factor of meters to kilometers will result in the
        // values being different by a multiplier of 1,000,000 (1,000 meters x 1,000 meters).

        // if unit is a meter, apply kilometers scale factor
        CoordinateReferenceSystem crs = pointFeatures.getSchema().getCoordinateReferenceSystem();
        if (crs != null && crs.getCoordinateSystem() != null) {
            CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
            if (!(hor instanceof GeographicCRS)) {
                Unit<?> unit = hor.getCoordinateSystem().getAxis(0).getUnit();
                // UnitConverter converter = SI.METER.getConverterTo(unit);
                if (unit != null && unit == SI.METRE) {
                    this.scaleArea = scaleArea / 1000000.0;
                }
            }
        }

        return createGridCoverage("PointDensity", scaleUnit(densityImage));
    }

    private KernelJAI getKernel(RasterNeighborhood rnh) {
        scaleArea = 0.0;

        KernelJAI kernel = null;

        switch (rnh.getNeighborType()) {
        case CIRCLE:
            int radius = (int) rnh.getRadius();
            if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                // convert map unit to cell unit
                double cellSize = Math.max(pixelSizeX, pixelSizeY);
                radius = (int) Math.floor(rnh.getRadius() / cellSize);
            }

            // Creates a circular kernel with width 2*radius + 1
            kernel = KernelFactory.createCircle(radius, ValueType.BINARY);
            break;
        case RECTANGLE:
            int rw = (int) rnh.getWidth();
            int rh = (int) rnh.getHeight();
            if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                // convert map unit to cell unit
                rw = (int) Math.floor(rnh.getWidth() / pixelSizeX);
                rh = (int) Math.floor(rnh.getHeight() / pixelSizeY);
            }

            // Creates a rectangular kernel where all elements have value 1.0.
            kernel = KernelFactory.createRectangle(2 * rw + 1, 2 * rh + 1);
            break;
        }

        // calculate area
        final double cellArea = pixelSizeX * pixelSizeY;
        final float[] data = kernel.getKernelData();
        int valid = 0;
        for (int index = 0; index < data.length; index++) {
            if (data[index] != 0.0) {
                scaleArea += cellArea;
                valid++;
            }
        }

        this.minValue = 0.0;
        this.maxValue = maxValue * valid;

        return kernel;
    }
}