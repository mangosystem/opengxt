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

import java.awt.Dimension;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessingException;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;

/**
 * Change the spatial resolution of raster and set rules for aggregating or interpolating values across the new pixel sizes.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterResampleOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterResampleOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem forcedCRS, double cellSize, ResampleType resamplingType)
            throws ProcessException {
        if (forcedCRS != null) {
            RasterForceCRSOperation process = new RasterForceCRSOperation();
            inputCoverage = process.execute(inputCoverage, forcedCRS);
        }

        return execute(inputCoverage, cellSize, cellSize, resamplingType);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem forcedCRS, double cellSizeX, double cellSizeY,
            ResampleType resamplingType) throws ProcessException {
        if (forcedCRS != null) {
            RasterForceCRSOperation process = new RasterForceCRSOperation();
            inputCoverage = process.execute(inputCoverage, forcedCRS);
        }

        return execute(inputCoverage, cellSizeX, cellSizeY, resamplingType);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, double cellSizeX, double cellSizeY,
            ResampleType resamplingType) throws ProcessException {
        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        switch (resamplingType) {
        case BICUBIC:
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            break;
        case BILINEAR:
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            break;
        default:
            interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            break;
        }

        // recalculate gridenvelope
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        ReferencedEnvelope extent = RasterHelper.getResolvedEnvelope(new ReferencedEnvelope(
                inputCoverage.getEnvelope()), cellSizeX, cellSizeY);

        Dimension dim = RasterHelper.getDimension(extent, cellSizeX, cellSizeY);
        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, dim.width, dim.height);
        GridGeometry2D gridGeometry = new GridGeometry2D(gridRange, extent);

        // execute resample
        try {
            return (GridCoverage2D) Operations.DEFAULT.resample(inputCoverage, crs, gridGeometry,
                    interpolation);
        } catch (CoverageProcessingException e) {
            throw new ProcessException(e);
        }
    }
}