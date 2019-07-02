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
import java.awt.geom.AffineTransform;
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
import org.geotools.process.spatialstatistics.core.UnitConverter;
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
        return execute(inputCoverage, targetCRS, resamplingType, 0.0);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, double cellSize)
            throws ProcessException {
        return execute(inputCoverage, targetCRS, resamplingType, cellSize, cellSize);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, double cellSizeX,
            double cellSizeY) throws ProcessException {
        return execute(inputCoverage, targetCRS, resamplingType, cellSizeX, cellSizeY, null);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage,
            CoordinateReferenceSystem targetCRS, ResampleType resamplingType, double cellSizeX,
            double cellSizeY, CoordinateReferenceSystem forcedCRS) throws ProcessException {
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

        if (cellSizeX <= 0 || cellSizeY <= 0) {
            // recalculate cell size
            GridGeometry2D gridGeometry2D = inputCoverage.getGridGeometry();
            AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

            cellSizeX = Math.abs(gridToWorld.getScaleX());
            cellSizeY = Math.abs(gridToWorld.getScaleY());

            // check Geographic CRS
            boolean sourceIsGeo = UnitConverter.isGeographicCRS(sourceCRS);
            boolean targetIsGeo = UnitConverter.isGeographicCRS(targetCRS);
            if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS) && (sourceIsGeo || targetIsGeo)) {
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

                cellSizeX = extent.getWidth() / columns;
                cellSizeY = extent.getHeight() / rows;
            }
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            GridGeometry2D gridGeometry2D = inputCoverage.getGridGeometry();
            AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

            double sourceX = Math.abs(gridToWorld.getScaleX());
            double sourceY = Math.abs(gridToWorld.getScaleY());

            if (SSUtils.compareDouble(sourceX, cellSizeX)
                    && SSUtils.compareDouble(sourceY, cellSizeY)) {
                return inputCoverage;
            } else {
                RasterResampleOperation resample = new RasterResampleOperation();
                return resample.execute(inputCoverage, cellSizeX, cellSizeY, resamplingType);
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

        extent = RasterHelper.getResolvedEnvelope(extent, cellSizeX, cellSizeY);
        Dimension dim = RasterHelper.getDimension(extent, cellSizeX, cellSizeY);
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