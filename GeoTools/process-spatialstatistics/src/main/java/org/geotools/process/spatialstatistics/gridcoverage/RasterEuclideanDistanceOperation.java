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

import java.util.Arrays;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Calculates, for each cell, the Euclidean distance to the closest source.
 * 
 * @author Minpa Lee, MangoSystem
 * @reference https://github.com/DotSpatial/DotSpatial/blob/master/Source/DotSpatial.Tools/RasterDistance.cs
 * 
 * @source $URL$
 */
public class RasterEuclideanDistanceOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterEuclideanDistanceOperation.class);

    static final int INT_MAX = Integer.MAX_VALUE;

    static final int INT_ZERO = 0;

    private double maximumDistance = Double.MAX_VALUE;

    private DiskMemImage outputImage;

    public GridCoverage2D execute(SimpleFeatureCollection inputFeatures, double maximumDistance) {
        FeaturesToRasterOperation process = new FeaturesToRasterOperation();
        final Number gridVal = Short.valueOf((short) 1);
        GridCoverage2D finalGc = null;

        // check features in this analysis extent
        ReferencedEnvelope extent = getRasterEnvironment().getExtent();
        if (extent != null) {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
            String the_geom = inputFeatures.getSchema().getGeometryDescriptor().getLocalName();

            Filter filter = ff.bbox(ff.property(the_geom), extent);
            int featureCount = inputFeatures.subCollection(filter).size();
            if (featureCount == 0) {
                // 1. feature to raster as origin extent
                process.getRasterEnvironment().setExtent(inputFeatures.getBounds());
                process.getRasterEnvironment().setCellSizeX(getRasterEnvironment().getCellSizeX());
                process.getRasterEnvironment().setCellSizeY(getRasterEnvironment().getCellSizeY());

                GridCoverage2D distGc = process.execute(inputFeatures, gridVal);

                // 2. crop raster with analysis extent
                RasterCropOperation cropOp = new RasterCropOperation();
                finalGc = cropOp.execute(distGc, extent);
            } else {
                process.setRasterEnvironment(getRasterEnvironment());
                finalGc = process.execute(inputFeatures, gridVal);
            }
        } else {
            process.setRasterEnvironment(getRasterEnvironment());
            finalGc = process.execute(inputFeatures, gridVal);
        }

        return execute(finalGc, maximumDistance);
    }

    public GridCoverage2D execute(GridCoverage2D valueCoverage, double maximumDistance) {
        if (maximumDistance <= 0 || Double.isNaN(maximumDistance)) {
            this.maximumDistance = Double.MAX_VALUE;
        } else {
            this.maximumDistance = maximumDistance;
        }

        // create raster
        outputImage = createDiskMemImage(valueCoverage, RasterPixelType.FLOAT);

        // declare two jagged arrays for storing the (Rx, Ry) vectors.
        // rX is distance from current cell to nearest target cell measured in the x-direction
        // rY is distance from current cell to nearest target cell measured in the y-direction
        // the actual distance can be calculated as sqrt(rX^2 + rY^2).
        // in the resulting distance raster we store the squared distance as well as the rX, rY
        // relative coordinates to improve computation speed

        PlanarImage inputImage = (PlanarImage) valueCoverage.getRenderedImage();
        final double inputNoData = RasterHelper.getNoDataValue(valueCoverage);

        // ====================================================================
        // initialize the arrays
        // ====================================================================
        final int numColumns = inputImage.getWidth() + 2;
        final int numRows = inputImage.getHeight() + 2;

        int[][] aRx = new int[numRows][];
        int[][] aRy = new int[numRows][];
        int[][] aSqDist = new int[numRows][];

        initializeArrays(inputImage, inputNoData, aRx, aRy, aSqDist, numColumns);

        // ====================================================================
        // raster distance calculation pass one - top to bottom, left to right
        // ====================================================================
        int[] aNcels = new int[4]; // the values of four neighbouring cells (W, NW, N, NE)
        int[] aDiff = new int[4]; // the | y coordinate distances to nearest target cell

        for (int row = 1; row < numRows; row++) {
            for (int col = 1; col < numColumns - 1; col++) {
                int val = aSqDist[row][col];

                // Continue processing only if the current cell is not a target
                if (val == INT_ZERO) {
                    continue;
                }

                // read the values of the cell's neighbours
                aNcels[0] = aSqDist[row][col - 1]; // W
                aNcels[1] = aSqDist[row - 1][col - 1]; // NW
                aNcels[2] = aSqDist[row - 1][col]; // N
                aNcels[3] = aSqDist[row - 1][col + 1]; // NE

                // calculate the squared euclidean distances to each neighbouring cell and to the
                // nearest target cell
                if (aNcels[0] < INT_MAX) {
                    aDiff[0] = aNcels[0] + 2 * aRx[row][col - 1] + 1;
                } else {
                    aDiff[0] = INT_MAX;
                }

                if (aNcels[1] < INT_MAX) {
                    aDiff[1] = aNcels[1] + 2 * (aRx[row - 1][col - 1] + aRy[row - 1][col - 1] + 1);
                } else {
                    aDiff[1] = INT_MAX;
                }

                if (aNcels[2] < INT_MAX) {
                    aDiff[2] = aNcels[2] + 2 * aRy[row - 1][col] + 1;
                } else {
                    aDiff[2] = INT_MAX;
                }

                if (aNcels[3] < INT_MAX) {
                    aDiff[3] = aNcels[3] + 2 * (aRx[row - 1][col + 1] + aRy[row - 1][col + 1] + 1);
                } else {
                    aDiff[3] = INT_MAX;
                }

                // find neighbouring cell with minimum distance difference
                int minDiff = aDiff[0];
                int minDiffCell = 0;
                for (int i = 1; i < 4; i++) {
                    if (aDiff[i] < minDiff) {
                        minDiff = aDiff[i];
                        minDiffCell = i;
                    }
                }

                // if a neighbouring cell with known distance was found:
                if (minDiff < INT_MAX) {
                    // assign the minimum euclidean distance
                    aSqDist[row][col] = minDiff;

                    // update the (rX, rY) cell-to-nearest-target vector
                    switch (minDiffCell) {
                    case 0: // W
                        aRx[row][col] = aRx[row][col - 1] + 1;
                        aRy[row][col] = aRy[row][col - 1];
                        break;
                    case 1: // NW
                        aRx[row][col] = aRx[row - 1][col - 1] + 1;
                        aRy[row][col] = aRy[row - 1][col - 1] + 1;
                        break;
                    case 2: // N
                        aRx[row][col] = aRx[row - 1][col];
                        aRy[row][col] = aRy[row - 1][col] + 1;
                        break;
                    case 3: // NE
                        aRx[row][col] = aRx[row - 1][col + 1] + 1;
                        aRy[row][col] = aRy[row - 1][col + 1] + 1;
                        break;
                    }
                }
                // end of update (rX, rY) cell-to-nearest-target vector
            }
            // end or current row processing
        }
        // ====================================================================
        // end of first pass for loop
        // ====================================================================

        // ====================================================================
        // raster distance calculation PASS TWO - bottom to top, right to left
        // ====================================================================
        for (int row = numRows - 2; row > 0; row--) {
            for (int col = numColumns - 2; col > 0; col--) {
                int val = aSqDist[row][col];

                // Continue processing only if the current cell is not a target
                if (val == INT_ZERO) {
                    continue;
                }

                // read the values of the cell's neighbours
                aNcels[0] = aSqDist[row][col + 1]; // E
                aNcels[1] = aSqDist[row + 1][col + 1]; // SE
                aNcels[2] = aSqDist[row + 1][col]; // S
                aNcels[3] = aSqDist[row + 1][col - 1]; // SW

                // calculate the squared euclidean distances to each neighbouring cell and to the
                // nearest target cell
                if (aNcels[0] < INT_MAX) {
                    aDiff[0] = aNcels[0] + 2 * aRx[row][col + 1] + 1;
                } else {
                    aDiff[0] = INT_MAX;
                }

                if (aNcels[1] < INT_MAX) {
                    aDiff[1] = aNcels[1] + 2 * (aRx[row + 1][col + 1] + aRy[row + 1][col + 1] + 1);
                } else {
                    aDiff[1] = INT_MAX;
                }

                if (aNcels[2] < INT_MAX) {
                    aDiff[2] = aNcels[2] + 2 * aRy[row + 1][col] + 1;
                } else {
                    aDiff[2] = INT_MAX;
                }

                if (aNcels[3] < INT_MAX) {
                    aDiff[3] = aNcels[3] + 2 * (aRx[row + 1][col - 1] + aRy[row + 1][col - 1] + 1);
                } else {
                    aDiff[3] = INT_MAX;
                }

                // find neighbouring cell with minimum distance difference
                int minDiff = aDiff[0];
                int minDiffCell = 0;
                for (int i = 1; i < 4; i++) {
                    if (aDiff[i] < minDiff) {
                        minDiff = aDiff[i];
                        minDiffCell = i;
                    }
                }

                // if a neighbouring cell with known distance smaller than current known distance
                // was found:
                if (minDiff < val) {
                    // assign the minimum euclidean distance
                    aSqDist[row][col] = minDiff;

                    // update the (rX, rY) cell-to-nearest-target vector
                    switch (minDiffCell) {
                    case 0: // E
                        aRx[row][col] = aRx[row][col + 1] + 1;
                        aRy[row][col] = aRy[row][col + 1];
                        break;
                    case 1: // SE
                        aRx[row][col] = aRx[row + 1][col + 1] + 1;
                        aRy[row][col] = aRy[row + 1][col + 1] + 1;
                        break;
                    case 2: // S
                        aRx[row][col] = aRx[row + 1][col];
                        aRy[row][col] = aRy[row + 1][col] + 1;
                        break;
                    case 3: // SW
                        aRx[row][col] = aRx[row + 1][col - 1] + 1;
                        aRy[row][col] = aRy[row + 1][col - 1] + 1;
                        break;
                    }
                }
                // end of update (rX, rY) cell-to-nearest-target vector
            }

            // write output distance
            writeDistance(row - 1, aSqDist[row]);
        }
        // *******************************************************************
        // end of second pass proximity calculation loop
        // *******************************************************************

        return createGridCoverage("EuclideanDistance", outputImage);
    }

    private void writeDistance(int row, int[] colValues) {
        final int length = colValues.length;
        for (int col = 0; col < length - 1; col++) {
            int val = colValues[col + 1];

            if (val == INT_MAX) {
                outputImage.setSample(col, row, 0, NoData);
            } else {
                double distance = Math.sqrt(val) * CellSizeX;
                if (maximumDistance < distance) {
                    outputImage.setSample(col, row, 0, NoData);
                } else {
                    outputImage.setSample(col, row, 0, distance);
                    updateStatistics(distance);
                }
            }
        }
    }

    private void initializeArrays(PlanarImage inputImage, double inputNoData, final int[][] aRx,
            final int[][] aRy, final int[][] aSqDist, int numColumns) {
        final RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        // first row
        int dy = 0;
        aRx[dy] = new int[numColumns];
        aRy[dy] = new int[numColumns];
        aSqDist[dy] = new int[numColumns];
        Arrays.fill(aSqDist[dy], INT_MAX);

        // valid cell
        dy++;
        readIter.startLines();
        while (!readIter.finishedLines()) {
            aRx[dy] = new int[numColumns];
            aRy[dy] = new int[numColumns];
            aSqDist[dy] = new int[numColumns];
            Arrays.fill(aSqDist[dy], INT_MAX);

            int dx = 1;
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                final int val = readIter.getSample(0);
                if (!SSUtils.compareDouble(val, inputNoData)) {
                    aSqDist[dy][dx] = INT_ZERO;
                }
                readIter.nextPixel();
                dx++;
            }
            readIter.nextLine();
            dy++;
        }

        // last row
        aRx[dy] = new int[numColumns];
        aRy[dy] = new int[numColumns];
        aSqDist[dy] = new int[numColumns];
        Arrays.fill(aSqDist[dy], INT_MAX);
    }
}