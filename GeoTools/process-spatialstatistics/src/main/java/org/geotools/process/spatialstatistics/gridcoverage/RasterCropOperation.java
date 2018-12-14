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
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

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
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();

        // check geometry
        GeometryCollection roi = null;
        if (cropShape == null || cropShape.isEmpty()) {
            roi = null;
        } else {
            Class<?> geomBinding = cropShape.getClass();
            if (geomBinding.isAssignableFrom(MultiPolygon.class)
                    || geomBinding.isAssignableFrom(Polygon.class)) {
                if (!cropShape.isValid() || !cropShape.isSimple()) {
                    cropShape = cropShape.buffer(0);
                    cropShape.setUserData(cropShape.getUserData());
                }
            }

            // transform if necessary
            cropShape = transformGeometry(cropShape, crs);

            // force it to a collection if necessary
            if (cropShape instanceof GeometryCollection) {
                roi = (GeometryCollection) cropShape;
            } else {
                roi = cropShape.getFactory().createGeometryCollection(new Geometry[] { cropShape });
            }
        }

        // check bounds
        GeneralEnvelope bounds = null;
        if (extent == null || extent.isEmpty() || extent.isNull()) {
            if (roi != null) {
                bounds = new GeneralEnvelope(new ReferencedEnvelope(roi.getEnvelopeInternal(), crs));
            }
        } else {
            CoordinateReferenceSystem extCrs = extent.getCoordinateReferenceSystem();
            if (!CRS.equalsIgnoreMetadata(crs, extCrs)) {
                try {
                    MathTransform transform = CRS.findMathTransform(extCrs, crs, true);
                    Envelope env = JTS.transform(extent, transform);
                    bounds = new GeneralEnvelope(new ReferencedEnvelope(env, crs));
                } catch (FactoryException e) {
                    throw new ProcessException(e);
                } catch (MismatchedDimensionException e) {
                    throw new ProcessException(e);
                } catch (TransformException e) {
                    throw new ProcessException(e);
                }
            } else {
                bounds = new GeneralEnvelope(extent);
            }
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