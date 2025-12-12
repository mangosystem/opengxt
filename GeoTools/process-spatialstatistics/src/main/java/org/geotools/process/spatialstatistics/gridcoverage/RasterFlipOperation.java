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

// JAI -> ImageN
// import javax.media.jai.JAI;
// import javax.media.jai.ParameterBlockJAI;

import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.ParameterBlockImageN;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.eclipse.imagen.operator.TransposeDescriptor;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.metadata.i18n.Vocabulary;
import org.geotools.metadata.i18n.VocabularyKeys;
import org.geotools.util.logging.Logging;

/**
 * Reorients the raster by turning it over, from top to bottom, along the
 * horizontal axis through the center of the raster.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFlipOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterFlipOperation.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public GridCoverage2D execute(GridCoverage2D inputCoverage) {
        this.initilizeVariables(inputCoverage);

        // input coverage -> ImageN PlanarImage
        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();

        // JAI ParameterBlockJAI -> ImageN ParameterBlockImageN
        //   new ParameterBlockJAI("transpose", "rendered");
        ParameterBlockImageN parameterBlock = new ParameterBlockImageN("transpose", "rendered");
        parameterBlock.addSource(inputImage);
        parameterBlock.setParameter("type", TransposeDescriptor.FLIP_VERTICAL);

        // JAI.create(...) -> ImageN.create(...)
        //  - ImageN는 JAI와 동일한 패턴의 create / createNS 를 제공
        //  - RenderedOp 는 PlanarImage 를 상속하므로 PlanarImage 로 바로 받을 수 있음
        PlanarImage outputImage = (PlanarImage) ImageN.create("transpose", parameterBlock);

        final int numBands = inputCoverage.getNumSampleDimensions();

        if (numBands == 1) {
            return createGridCoverage(inputCoverage.getName(), outputImage);
        } else {
            GridSampleDimension[] bands = inputCoverage.getSampleDimensions();

            double[] nodataValues = bands[0].getNoDataValues();
            Object noData = nodataValues == null ? Integer.MAX_VALUE : nodataValues[0];

            Map properties = inputCoverage.getProperties();
            properties.put(Vocabulary.formatInternational(VocabularyKeys.NODATA), noData);
            properties.put(NoDataContainer.GC_NODATA, noData);

            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            return factory.create(
                    inputCoverage.getName(),
                    outputImage,
                    gridExtent,
                    bands,
                    null,
                    properties);
        }
    }
}
