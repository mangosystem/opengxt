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

import java.awt.RenderingHints;
import java.util.logging.Logger;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.gridcoverage.RasterNeighborhood.NeighborUnits;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

/**
 * Calculates a magnitude per unit area from point features that fall within a neighborhood around each cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterPointDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterKernelDensityOperation.class);

    // Silverman, B.W. Density Estimation for Statistics and Data Analysis. New York: Chapman and
    // Hall, 1986.

    // default = circle, 8 * 8 cell unit
    RasterNeighborhood rnh = new RasterNeighborhood();

    public RasterPointDensityOperation() {
        // default setting
        rnh.setCircle(8.0, NeighborUnits.CELL);
    }

    public void setNeighbor(RasterNeighborhood neighborhood) {
        this.rnh = neighborhood;
    }

    public RasterNeighborhood getNeighbor() {
        return rnh;
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField) {
        // step 1 : convert point to gridcoverage : Sum
        final PlanarImage outputImage = pointToRaster(pointFeatures, weightField);

        // step 2 Only a circular neighborhood is possible
        final KernelJAI kernel = getKernel(this.rnh);

        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        final ParameterBlockJAI pb = new ParameterBlockJAI("Convolve");
        pb.setSource("source0", outputImage);
        pb.setParameter("kernel", kernel);

        PlanarImage densityImage = JAI.create("Convolve", pb, hints);

        // If an area unit is selected, the calculated density for the cell is multiplied by the
        // appropriate factor before it is written to the output raster.
        // For example, if the input units are meters, the output area units will default to square
        // kilometers. Comparing a unit scale factor of meters to kilometers will result in the
        // values being different by a multiplier of 1,000,000 (1,000 meters x 1,000 meters).

        // if unit is a meter, apply kilometers scale factor
        CoordinateReferenceSystem crs = pointFeatures.getSchema().getCoordinateReferenceSystem();
        if (crs != null && crs.getCoordinateSystem() != null) {
            CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
            if (!(hor instanceof GeographicCRS)) {
                Unit<?> unit = hor.getCoordinateSystem().getAxis(0).getUnit();
                // UnitConverter converter = SI.METER.getConverterTo(unit);
                if (unit != null && unit == SI.METRE) {
                    this.scaleArea = scaleArea / 1000000.0;
                }
            }
        }

        densityImage = scaleUnit(densityImage);

        return createGridCoverage("PointDensity", densityImage);
    }

    private KernelJAI getKernel(RasterNeighborhood rnh) {
        scaleArea = 0.0;

        KernelJAI kernel = null;

        switch (rnh.getNeighborType()) {
        case CIRCLE:
            int radius = (int) rnh.getRadius();
            if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                // convert map unit to cell unit
                radius = (int) Math.floor(rnh.getRadius() / CellSize);
            }

            // Creates a circular kernel with width 2*radius + 1
            kernel = KernelFactory.createCircle(radius, ValueType.BINARY);
            break;
        case RECTANGLE:
            int rw = (int) rnh.getWidth();
            int rh = (int) rnh.getHeight();
            if (rnh.getNeighborUnits() == NeighborUnits.MAP) {
                // convert map unit to cell unit
                rw = (int) Math.floor(rnh.getWidth() / CellSize);
                rh = (int) Math.floor(rnh.getHeight() / CellSize);
            }

            // Creates a rectangular kernel where all elements have value 1.0.
            kernel = KernelFactory.createRectangle(2 * rw + 1, 2 * rh + 1);
            break;
        }

        // calculate area
        final double cellArea = CellSize * CellSize;
        final float[] data = kernel.getKernelData();
        for (int index = 0; index < data.length; index++) {
            if (data[index] != 0.0) {
                scaleArea += cellArea;
            }
        }

        return kernel;
    }
}