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

import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.logging.Logging;

/**
 * Reorients the raster by turning it over, from top to bottom, along the horizontal axis through the center of the raster.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFlipOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterFlipOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputCoverage) {
        this.initilizeVariables(inputCoverage);

        // transpose parameters: type
        // http://download.java.net/media/jai/javadoc/1.1.3/jai-apidocs/javax/media/jai/operator/TransposeDescriptor.html
        // http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/Geom-image-manip.doc.html#51140
        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        ParameterBlockJAI parameterBlock = new ParameterBlockJAI("transpose", "rendered");
        parameterBlock.addSource(inputImage);

        parameterBlock.setParameter("type", TransposeDescriptor.FLIP_VERTICAL);
        PlanarImage outputImage = JAI.create("transpose", parameterBlock);

        return createGridCoverage(inputCoverage.getName(), outputImage);
    }
}
