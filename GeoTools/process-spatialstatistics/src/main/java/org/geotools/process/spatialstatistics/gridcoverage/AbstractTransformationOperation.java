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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Abstract Transformation Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractTransformationOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractTransformationOperation.class);

    protected void initilizeVariables(GridCoverage2D inputCoverage) {
        NoData = RasterHelper.getNoDataValue(inputCoverage);
        CellSize = RasterHelper.getCellSize(inputCoverage);
        MaxValue = 1;
        MinValue = 0;
        Extent = new ReferencedEnvelope(inputCoverage.getEnvelope());
    }
}