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

import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geotools.api.parameter.InvalidParameterValueException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.metadata.i18n.Vocabulary;
import org.geotools.metadata.i18n.VocabularyKeys;
import org.geotools.util.logging.Logging;

import it.geosolutions.jaiext.range.NoDataContainer;

/**
 * Resizes a raster by the specified x and y scale factors.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRescaleOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterRescaleOperation.class);

    /**
     * Resizes a raster by the specified x and y scale factors.
     * 
     * @param inputCoverage The input raster.
     * @param xScale The factor in which to scale the cell size in the x direction. The factor must be greater than zero.
     * @param yScale The factor in which to scale the cell size in the y direction. The factor must be greater than zero.
     * @return GridCoverage2D
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GridCoverage2D execute(GridCoverage2D inputCoverage, double xScale, double yScale) {
        if (xScale <= 0) {
            throw new InvalidParameterValueException("xScale must be greater than zero", "xScale",
                    xScale);
        }

        if (yScale <= 0) {
            throw new InvalidParameterValueException("yScale must be greater than zero", "yScale",
                    yScale);
        }

        this.initilizeVariables(inputCoverage);

        final int numBands = inputCoverage.getNumSampleDimensions();

        ReferencedEnvelope extent = new ReferencedEnvelope(inputCoverage.getEnvelope());
        GridGeometry2D gridGeometry2D = inputCoverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();
        pixelSizeX = Math.abs(gridToWorld.getScaleX()) * xScale;
        pixelSizeY = Math.abs(gridToWorld.getScaleY()) * yScale;

        // 1. The output size is multiplied by the scale factor for both the x and y directions. The
        // number of columns and rows stays the same in this process, but the cell size is
        // multiplied by the scale factor.
        // 2. The scale factor must be positive.
        // 3. A scale factor greater than one means the image will be rescaled to a larger
        // dimension,
        // resulting in a larger extent because of a larger cell size.
        // 4. A scale factor less than one means the image will be rescaled to a smaller dimension,
        // resulting in a smaller extent because of a smaller cell size.

        // Rescale extent
        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        double maxX = extent.getMinX() + (inputImage.getWidth() * pixelSizeX);
        double maxY = extent.getMinY() + (inputImage.getHeight() * pixelSizeY);

        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        gridExtent = new ReferencedEnvelope(extent.getMinX(), maxX, extent.getMinY(), maxY, crs);

        if (numBands == 1) {
            return createGridCoverage(inputCoverage.getName(), inputImage);
        } else {
            GridSampleDimension[] bands = inputCoverage.getSampleDimensions();

            double[] nodataValues = bands[0].getNoDataValues();
            Object noData = nodataValues == null ? Integer.MAX_VALUE : nodataValues[0];

            Map properties = inputCoverage.getProperties();
            properties.put(Vocabulary.formatInternational(VocabularyKeys.NODATA), noData);
            properties.put(NoDataContainer.GC_NODATA, noData);

            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            return factory.create(inputCoverage.getName(), inputImage, gridExtent, bands, null,
                    properties);
        }
    }
}
