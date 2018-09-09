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

import it.geosolutions.jaiext.JAIExt;

import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * Creates a spatial subset of a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterCropOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterCropOperation.class);

    static final double roiTolerance = 0.0;

    static {
        JAIExt.initJAIEXT();
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Geometry cropShape) {
        // get the bounds
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();

        cropShape = transformGeometry(cropShape, crs);
        ReferencedEnvelope envelope = new ReferencedEnvelope(cropShape.getEnvelopeInternal(), crs);

        return execute(inputCoverage, cropShape, envelope);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, ReferencedEnvelope extent) {
        GeneralEnvelope bounds = new GeneralEnvelope(extent);

        // perform the crops
        final CoverageProcessor coverageProcessor = CoverageProcessor.getInstance();
        final Operation cropOperation = coverageProcessor.getOperation("CoverageCrop");

        final ParameterValueGroup param = cropOperation.getParameters();
        param.parameter("Source").setValue(inputCoverage);
        param.parameter("Envelope").setValue(bounds);
        param.parameter("ROITolerance").setValue(roiTolerance); // default

        return doOperation(param, null);
    }

    public GridCoverage2D execute(GridCoverage2D inputCoverage, Geometry cropShape,
            ReferencedEnvelope extent) {
        GeneralEnvelope bounds = new GeneralEnvelope(extent);
        cropShape = transformGeometry(cropShape, inputCoverage.getCoordinateReferenceSystem());

        // force it to a collection if necessary
        GeometryCollection roi = null;
        if (!(cropShape instanceof GeometryCollection)) {
            roi = cropShape.getFactory().createGeometryCollection(new Geometry[] { cropShape });
        } else {
            roi = (GeometryCollection) cropShape;
        }

        // perform the crops
        final CoverageProcessor coverageProcessor = CoverageProcessor.getInstance();
        final Operation cropOperation = coverageProcessor.getOperation("CoverageCrop");

        final ParameterValueGroup param = cropOperation.getParameters();
        param.parameter("Source").setValue(inputCoverage);
        param.parameter("Envelope").setValue(bounds);
        param.parameter("ROI").setValue(roi);
        param.parameter("ROITolerance").setValue(roiTolerance); // default

        return doOperation(param, null);
    }

    private GridCoverage2D doOperation(ParameterValueGroup parameters, Hints hints) {
        Crop cropOp = new Crop();
        return (GridCoverage2D) cropOp.doOperation(parameters, hints);
    }
}