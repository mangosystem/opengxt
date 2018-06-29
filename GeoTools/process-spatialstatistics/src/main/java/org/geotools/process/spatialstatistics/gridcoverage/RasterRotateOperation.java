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

import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * Turns a raster dataset around the specified pivot point by the angle specified angle in degrees. <br>
 * the raster dataset will rotate in a clockwise direction.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterRotateOperation extends AbstractTransformationOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterRotateOperation.class);

    /**
     * The resampling algorithm to be used. The default is NEAREST.
     */
    Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    public Interpolation getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, double angle) {
        // The default is the lower left corner of the input raster dataset.
        DirectPosition origin = inputCoverage.getEnvelope().getLowerCorner();
        Coordinate anchorPoint = new Coordinate(origin.getOrdinate(0), origin.getOrdinate(1));

        return execute(inputCoverage, anchorPoint, angle);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Point anchorPoint, double angle) {
        return execute(inputCoverage, anchorPoint.getCoordinate(), angle);
    }

    /**
     * Turns a raster dataset around the specified pivot point by the angle specified angle in degrees.
     * 
     * @param inputCoverage inputCoverage The input raster dataset.
     * @param anchorPoint The pivot point around which to rotate the raster. The default is the lower left corner of the input raster dataset.
     * @param angle The angle in degrees to rotate the raster. This can be any floating-point number.
     * @return GridCoverage2D
     */
    public GridCoverage2D execute(GridCoverage2D inputCoverage, Coordinate anchorPoint, double angle) {
        this.initilizeVariables(inputCoverage);

        final PlanarImage inputImage = (PlanarImage) inputCoverage.getRenderedImage();
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        DirectPosition realPos = new DirectPosition2D(crs, anchorPoint.x, anchorPoint.y);

        // The default is the lower left corner of the input raster dataset.
        DirectPosition gridPos = new DirectPosition2D(-0.5, inputImage.getHeight() - 0.5);
        try {
            gridPos = RasterHelper.worldToGridPos(inputCoverage, realPos);
        } catch (TransformException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        // Rotate_management (in_raster, out_raster, angle, {pivot_point}, {resampling_type})
        // http://resources.arcgis.com/en/help/main/10.1/#/Rotate/00170000007s000000/
        // http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/Geom-image-manip.doc.html#51140

        // rotate = [xOrigin, yOrigin, angle, interpolation, backgroundValues]
        ParameterBlockJAI parameterBlock = new ParameterBlockJAI("rotate", "rendered");
        parameterBlock.addSource(inputImage);

        parameterBlock.setParameter("xOrigin", (float) gridPos.getOrdinate(0) + 0.5f);
        parameterBlock.setParameter("yOrigin", (float) gridPos.getOrdinate(1) + 0.5f);

        float rotZRadians = (float) SSUtils.convert2Radians(angle);
        parameterBlock.setParameter("angle", rotZRadians);

        parameterBlock.setParameter("interpolation", interpolation);

        final double[] backgroundValues = new double[inputImage.getSampleModel().getNumBands()];
        for (int index = 0; index < backgroundValues.length; index++) {
            backgroundValues[index] = NoData;
        }
        parameterBlock.setParameter("backgroundValues", backgroundValues);

        PlanarImage outputImage = JAI.create("rotate", parameterBlock);

        // rotate envelope
        Envelope newExt = new ReferencedEnvelope(inputCoverage.getEnvelope());
        try {
            // Important!, the raster dataset will rotate in a clockwise direction.
            rotZRadians = (float) SSUtils.convert2Radians(360 - angle);

            AffineTransform affineTransform = AffineTransform.getRotateInstance(rotZRadians,
                    anchorPoint.x, anchorPoint.y);
            MathTransform mathTransform = new AffineTransform2D(affineTransform);

            newExt = JTS.transform(Extent, mathTransform);
        } catch (MismatchedDimensionException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        // adjust extent
        final int column = outputImage.getWidth();
        final int row = outputImage.getHeight();
        final double maxX = newExt.getMinX() + (column * CellSize);
        final double maxY = newExt.getMinY() + (row * CellSize);

        Extent = new ReferencedEnvelope(newExt.getMinX(), maxX, newExt.getMinY(), maxY, crs);

        return createGridCoverage(inputCoverage.getName(), outputImage);
    }
}
