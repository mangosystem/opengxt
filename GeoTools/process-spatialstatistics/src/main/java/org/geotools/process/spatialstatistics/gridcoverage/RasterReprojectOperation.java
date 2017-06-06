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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessingException;
import org.geotools.coverage.processing.Operations;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Reprojects the raster dataset from one projection to another.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReprojectOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterReprojectOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem forcedCRS, CoordinateReferenceSystem targetCRS,
            ResampleType resamplingType) throws ProcessException {
        RasterForceCRSOperation process = new RasterForceCRSOperation();
        GridCoverage2D definedGC = process.execute(inputCoverage, forcedCRS);

        return execute(definedGC, targetCRS, resamplingType);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType)
            throws ProcessException {
        CoordinateReferenceSystem sourceCRS = inputCoverage.getCoordinateReferenceSystem();
        if (sourceCRS == null) {
            throw new ProcessException("inputCoverage has no CRS!");
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            LOGGER.log(Level.WARNING, "inputCoverage's CRS equals targetCRS!");
            return inputCoverage;
        }

        GridCoverage2D resultGc = null;

        Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        switch (resamplingType) {
        case BICUBIC:
            interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            break;
        case BILINEAR:
            interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            break;
        default:
            interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            break;
        }

        // execute resample
        try {
            resultGc = (GridCoverage2D) Operations.DEFAULT.resample(inputCoverage, targetCRS, null,
                    interp);
        } catch (CoverageProcessingException e) {
            LOGGER.log(Level.ALL, e.getMessage(), e);
        }

        return resultGc;
    }
}