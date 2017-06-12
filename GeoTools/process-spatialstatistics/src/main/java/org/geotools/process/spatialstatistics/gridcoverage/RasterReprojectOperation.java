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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessingException;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Reprojects the raster dataset from one projection to another.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterReprojectOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterReprojectOperation.class);

    public GridCoverage2D execute(GridCoverage2D inputCoverage, CoordinateReferenceSystem targetCRS)
            throws ProcessException {
        return execute(inputCoverage, targetCRS, ResampleType.NEAREST);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType)
            throws ProcessException {
        return execute(inputCoverage, targetCRS, ResampleType.NEAREST, 0.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, double cellSize)
            throws ProcessException {
        return execute(inputCoverage, targetCRS, ResampleType.NEAREST, cellSize, null);
    }

    private double getCellSize(GridCoverage2D inputCoverage, CoordinateReferenceSystem targetCRS) {
        double cellSize = RasterHelper.getCellSize(inputCoverage);

        // check Geographic CRS
        CoordinateReferenceSystem sCRS = inputCoverage.getCoordinateReferenceSystem();
        if (sCRS instanceof DefaultGeographicCRS) {
            // recalculate cell size
            ReferencedEnvelope extent = null;
            try {
                ReferencedEnvelope bounds = new ReferencedEnvelope(inputCoverage.getEnvelope());
                extent = bounds.transform(targetCRS, true);
            } catch (TransformException e) {
                throw new ProcessException(e);
            } catch (FactoryException e) {
                throw new ProcessException(e);
            }

            GridEnvelope grid = inputCoverage.getGridGeometry().getGridRange();
            int columns = grid.getHigh(0) + 1;
            int rows = grid.getHigh(1) + 1;

            double sizeX = extent.getWidth() / columns;
            double sizeY = extent.getHeight() / rows;

            // use minimum value!
            cellSize = Math.min(sizeX, sizeY);
        }

        return cellSize;
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, double cellSize,
            CoordinateReferenceSystem forcedCRS) throws ProcessException {
        if (targetCRS == null) {
            throw new ProcessException("targetCRS is null!");
        }

        // check forcedCRS
        if (forcedCRS != null) {
            RasterForceCRSOperation process = new RasterForceCRSOperation();
            inputCoverage = process.execute(inputCoverage, forcedCRS);
        }

        CoordinateReferenceSystem sourceCRS = inputCoverage.getCoordinateReferenceSystem();
        if (sourceCRS == null) {
            throw new ProcessException("inputCoverage has no CRS!");
        }

        if (cellSize <= 0) {
            cellSize = getCellSize(inputCoverage, targetCRS);
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            double sourceCellSize = RasterHelper.getCellSize(inputCoverage);
            if (SSUtils.compareDouble(cellSize, sourceCellSize)) {
                return inputCoverage;
            } else {
                RasterResampleOperation resample = new RasterResampleOperation();
                return resample.execute(inputCoverage, cellSize, resamplingType);
            }
        }

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
        ReferencedEnvelope srcEnv = new ReferencedEnvelope(inputCoverage.getEnvelope());

        ReferencedEnvelope extent = null;
        try {
            extent = srcEnv.transform(targetCRS, true);
        } catch (TransformException e) {
            throw new ProcessException(e);
        } catch (FactoryException e) {
            throw new ProcessException(e);
        }

        extent = RasterHelper.getResolvedEnvelope(extent, cellSize);
        Dimension dim = RasterHelper.getDimension(extent, cellSize);
        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, dim.width, dim.height);
        GridGeometry2D gridGeometry = new GridGeometry2D(gridRange, extent);

        // execute resample
        try {
            return (GridCoverage2D) Operations.DEFAULT.resample(inputCoverage, targetCRS,
                    gridGeometry, interpolation);
        } catch (CoverageProcessingException e) {
            throw new ProcessException(e);
        }
    }
}