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
package org.geotools.process.spatialstatistics.pattern;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.imagen.KernelImageN;
import org.eclipse.imagen.RasterFactory;
import org.eclipse.imagen.media.kernel.KernelFactory;
import org.eclipse.imagen.media.kernel.KernelFactory.ValueType;
import org.eclipse.imagen.media.kernel.KernelUtil;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.gridcoverage.GridTransformer;
import org.geotools.util.logging.Logging;

/**
 * Circles to Raster Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @see https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public class CirclesToDensityRaster {
    protected static final Logger LOGGER = Logging.getLogger(CirclesToDensityRaster.class);

    private final ReferencedEnvelope extent;

    private final double cellSize;

    private final int height;

    private final int width;

    private final float[] pixels;

    private GridTransformer transform;

    private boolean standardize = false; // kernel standardize

    public CirclesToDensityRaster(ReferencedEnvelope extent) {
        this(extent, 0d);
    }

    private double getDefaultCellSize(ReferencedEnvelope extent) {
        CoordinateReferenceSystem crs = extent.getCoordinateReferenceSystem();
        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crs);
        double cellSize = Math.max(extent.getWidth(), extent.getHeight()) / 500.0;
        if (isGeographicCRS) {
            return cellSize;
        } else {
            return Math.ceil(cellSize);
        }
    }

    public CirclesToDensityRaster(ReferencedEnvelope extent, double cellSize) {
        if (cellSize == 0d) {
            cellSize = getDefaultCellSize(extent);
        }

        this.cellSize = cellSize;
        this.transform = new GridTransformer(extent, cellSize, cellSize);

        this.width = (int) Math.floor((extent.getWidth() / cellSize) + 0.5);
        this.height = (int) Math.floor((extent.getHeight() / cellSize) + 0.5);

        // recalculate extent
        double maxX = extent.getMinX() + (width * cellSize);
        double maxY = extent.getMinY() + (height * cellSize);

        this.extent = new ReferencedEnvelope(extent.getMinX(), maxX, extent.getMinY(), maxY,
                extent.getCoordinateReferenceSystem());

        this.pixels = new float[width * height];
    }

    public double getCellsize() {
        return this.cellSize;
    }

    public boolean isStandardize() {
        return standardize;
    }

    public void setStandardize(boolean standardize) {
        this.standardize = standardize;
    }

    public GridCoverage2D processCircles(List<ClusterCircle> circles) {
        for (ClusterCircle circle : circles) {
            quantize(circle);
        }

        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);

        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width,
                height, 1, null);
        raster.setPixels(0, 0, width, height, pixels);

        return gcf.create("GAM", raster, extent);
    }

    private void quantize(ClusterCircle circle) {
        float centreValue = (float) circle.getFitness();
        int radius = (int) (Math.floor(((circle.getRadius() + cellSize))) / cellSize);

        // Setup kernel
        KernelImageN kernel = KernelFactory.createCircle(radius, ValueType.EPANECHNIKOV);

        int w = kernel.getWidth();
        int h = kernel.getHeight();

        float[] data = kernel.getKernelData();
        data[radius * w + radius] = centreValue;

        kernel = new KernelImageN(w, h, kernel.getXOrigin(), kernel.getYOrigin(), data);

        if (standardize) {
            // kernel with element values standardized to sum to 1.0
            kernel = KernelUtil.standardize(kernel);
        }

        data = kernel.getKernelData();

        // World to Grid
        double x = circle.getCenter().getX();
        double y = circle.getCenter().getY();
        GridCoordinates2D gridPos = transform.worldToGrid(x, y);

        int k = 0;
        for (int iX = -radius; iX <= radius; iX++) {
            int dx = gridPos.x + iX;
            for (int iY = -radius; iY <= radius; iY++, k++) {
                if (data[k] == 0d) {
                    continue;
                }

                int dy = gridPos.y + iY;
                pixels[dy * this.width + dx] += data[k] * centreValue;
            }
        }
    }
}