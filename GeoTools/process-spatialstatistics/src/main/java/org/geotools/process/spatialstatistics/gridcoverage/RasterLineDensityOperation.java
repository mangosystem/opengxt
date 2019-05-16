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

import javax.measure.Unit;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import si.uom.SI;

/**
 * Calculates the density of linear features in the neighborhood of each output raster cell. <br>
 * Density is calculated in units of length per unit of area.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterLineDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterLineDensityOperation.class);

    public GridCoverage2D execute(SimpleFeatureCollection lineFeatures, String polulationField,
            double searchRadius) {
        // step 1 : convert line to gridcoverage
        final PlanarImage outputImage = lineToRaster(lineFeatures, polulationField);

        // Density = ((L1 * V1) + (L2 * V2)) / (area_of_circle)
        final KernelJAI kernel = getKernel(searchRadius);
        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        final ParameterBlockJAI pb = new ParameterBlockJAI("Convolve");
        pb.setSource("source0", outputImage);
        pb.setParameter("kernel", kernel);

        PlanarImage densityImage = JAI.create("Convolve", pb, hints);

        // If the linear unit is meters, the output area units will default to SQUARE_KILOMETERS
        // and the resulting line density units will convert to
        // kilometers per square kilometer.

        CoordinateReferenceSystem crs = lineFeatures.getSchema().getCoordinateReferenceSystem();
        if (crs != null && crs.getCoordinateSystem() != null) {
            CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
            if (!(hor instanceof GeographicCRS)) {
                Unit<?> unit = hor.getCoordinateSystem().getAxis(0).getUnit();
                // UnitConverter converter = SI.METER.getConverterTo(unit);
                if (unit != null && unit == SI.METRE) {
                    this.scaleArea = scaleArea / 1000.0;
                }
            }
        }

        return createGridCoverage("LineDensity", scaleUnit(densityImage));
    }

    private KernelJAI getKernel(double searchRadius) {
        scaleArea = 0.0;

        // convert map unit to cell unit
        double cellSize = Math.max(CellSizeX, CellSizeY);
        int radius = (int) Math.floor(searchRadius / cellSize);

        // Creates a circular kernel with width 2*radius + 1
        KernelJAI kernel = KernelFactory.createConstantCircle(radius, (float) cellSize);

        // calculate area
        final double cellArea = CellSizeX * CellSizeY;
        final float[] data = kernel.getKernelData();
        int valid = 0;
        for (int index = 0; index < data.length; index++) {
            if (data[index] != 0.0) {
                scaleArea += cellArea;
                valid++;
            }
        }

        this.MinValue = 0.0;
        this.MaxValue = MaxValue * valid;

        return kernel;
    }
}