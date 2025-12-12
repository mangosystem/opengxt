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

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.metadata.i18n.Vocabulary;
import org.geotools.metadata.i18n.VocabularyKeys;
import org.geotools.util.logging.Logging;

/**
 * Moves (slides) the raster to a new geographic location, based on x and y shift values.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterShiftOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterShiftOperation.class);

    /**
     * Moves (slides) the raster to a new geographic location, based on x and y shift values.
     * 
     * @param inputCoverage The input raster dataset.
     * @param xTrans The value used to shift the x coordinates.
     * @param yTrans The value used to shift the y coordinates.
     * @return GridCoverage2D
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GridCoverage2D execute(GridCoverage2D inputCoverage, double xTrans, double yTrans) {
        if (xTrans == 0 && yTrans == 0) {
            return inputCoverage;
        }

        this.initilizeVariables(inputCoverage);

        final int numBands = inputCoverage.getNumSampleDimensions();
        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();

        // 1. The cell size of the output raster will be the same as that of the input raster.
        // 2. The number of rows and columns in the output raster will be the same as those of the
        // input raster, no matter what parameters are specified.
        // 3. The coordinates of the lower left corner of the output raster will be offset from the
        // input raster by the x and y shift coordinate values specified.
        // 4. Using a negative shift x-coordinate value will shift the output to the left. A
        // positive shift x-coordinate value will shift the output to the right. Using a negative
        // shift y-coordinate value will shift the output down. A positive shift y-coordinate value
        // will shift the output to the top.

        gridExtent = new ReferencedEnvelope(inputCoverage.getEnvelope());
        gridExtent.translate(xTrans, yTrans);

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
