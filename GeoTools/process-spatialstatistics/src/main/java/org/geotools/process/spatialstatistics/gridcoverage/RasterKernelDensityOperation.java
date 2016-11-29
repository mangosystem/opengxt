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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

/**
 * Calculates a magnitude per unit area from point features using a kernel function to fit a smoothly tapered surface to each point.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterKernelDensityOperation extends RasterDensityOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterKernelDensityOperation.class);

    private KernelType kernelType = KernelType.Quadratic;

    public KernelType getKernelType() {
        return kernelType;
    }

    public void setKernelType(KernelType kernelType) {
        this.kernelType = kernelType;
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField) {
        // The default is the shortest of the width or height of the extent of in_features
        // in the output spatial reference, divided by 30
        ReferencedEnvelope extent = pointFeatures.getBounds();
        double searchRadius = Math.min(extent.getWidth(), extent.getHeight()) / 30.0;
        return execute(pointFeatures, weightField, searchRadius);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String weightField,
            double searchRadius) {
        // step 1 : convert point to gridcoverage : Sum
        final PlanarImage outputImage = pointToRaster(pointFeatures, weightField);

        // The kernel function is based on the quadratic kernel function described in Silverman
        // (1986, p. 76, equation 4.5).
        // http://arxiv.org/ftp/physics/papers/0701/0701111.pdf

        // step 2 Only a circular neighborhood is possible
        final KernelJAI kernel = getKernel(searchRadius);

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

        return createGridCoverage("KernelDensity", densityImage);
    }

    private KernelJAI getKernel(double searchRadius) {
        scaleArea = 0.0;

        // http://en.wikipedia.org/wiki/Kernel_(statistics)
        final int radius = (int) Math.floor(searchRadius / CellSize);
        final int width = 2 * radius + 1;
        final double r2 = radius * radius;

        // calculate area
        final double cellArea = CellSize * CellSize;

        // build kernel
        final KernelJAI binKernel = KernelFactory.createCircle(radius, ValueType.BINARY);

        // use cell's area
        final float[] data = binKernel.getKernelData();
        for (int index = 0; index < data.length; index++) {
            if (data[index] != 0.0) {
                scaleArea += cellArea;
            }
        }

        // area of circle
        // scaleArea = Math.PI * searchRadius * searchRadius;

        KernelJAI kernel = null;
        switch (this.kernelType) {
        case Binary:
            kernel = KernelFactory.createCircle(radius, ValueType.BINARY);
            break;
        case Cosine:
            kernel = KernelFactory.createCircle(radius, ValueType.COSINE);
            break;
        case Distance:
            kernel = KernelFactory.createCircle(radius, ValueType.DISTANCE);
            break;
        case Epanechnikov:
            kernel = KernelFactory.createCircle(radius, ValueType.EPANECHNIKOV);
            break;
        case Gaussian:
            kernel = KernelFactory.createCircle(radius, ValueType.GAUSSIAN);
            break;
        case InverseDistance:
            kernel = KernelFactory.createCircle(radius, ValueType.INVERSE_DISTANCE);
            break;
        case Quadratic:
            float[] weights = new float[width * width];
            for (int dY = -radius; dY <= radius; dY++) {
                final double dy2 = dY * dY;
                for (int dX = -radius; dX <= radius; dX++) {
                    final int index = (dY + radius) * width + (dX + radius);
                    if (data[index] == 0.0) {
                        weights[index] = 0;
                    } else {
                        // Silverman(1986, p. 76, equation 4.5).
                        final double dxdy = (dX * dX) + dy2;
                        final double termq = 1.0 - (dxdy / r2);
                        final double kde = 3.0 * termq * termq;

                        weights[index] = (float) kde;
                    }
                }
            }
            kernel = new KernelJAI(width, width, weights);
            break;
        case Quartic:
            kernel = KernelFactory.createCircle(radius, ValueType.QUARTIC);
            break;
        case Triangular:
            kernel = KernelFactory.createCircle(radius, ValueType.TRIANGULAR);
            break;
        case Triweight:
            kernel = KernelFactory.createCircle(radius, ValueType.TRIWEIGHT);
            break;
        case Tricube:
            // http://en.wikipedia.org/wiki/Kernel_(statistics)
            final double C_TRICUBE = 70.0 / 81.0;
            float[] tcWeights = new float[width * width];
            for (int dY = -radius; dY <= radius; dY++) {
                final double dy2 = dY * dY;
                for (int dX = -radius; dX <= radius; dX++) {
                    final int index = (dY + radius) * width + (dX + radius);
                    if (data[index] == 0.0) {
                        tcWeights[index] = 0;
                    } else {
                        final double dxdy = (dX * dX) + dy2;
                        final double u = Math.abs(Math.sqrt(dxdy) / radius);

                        final double termq = 1.0 - (u * u * u);
                        final double kde = C_TRICUBE * termq * termq * termq;

                        tcWeights[index] = (float) kde;
                    }
                }
            }
            kernel = new KernelJAI(width, width, tcWeights);
            break;
        }

        return kernel;
    }
}
