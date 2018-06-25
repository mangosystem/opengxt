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

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Extracts the cell values of a raster based on a set of point features and records the values in the attribute table of an output feature class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExtractValuesToPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterExtractValuesToPointsOperation.class);

    public enum ExtractionType {
        Default, SlopeAsDegree, SlopeAsPercentrise, Aspect
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String valueField, GridCoverage2D surfaceRaster) throws IOException {
        return execute(inputFeatures, valueField, surfaceRaster, ExtractionType.Default);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String valueField, GridCoverage2D surfaceRaster, ExtractionType valueType)
            throws IOException {
        // check crs
        CoordinateReferenceSystem sCRS = surfaceRaster.getCoordinateReferenceSystem();
        CoordinateReferenceSystem tCRS = inputFeatures.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(sCRS, tCRS)) {
            // reproject raster
            RasterReprojectOperation op = new RasterReprojectOperation();
            surfaceRaster = op.execute(surfaceRaster, tCRS, ResampleType.NEAREST);
        }

        SimpleFeatureType inputSchema = inputFeatures.getSchema();

        // prepare feature type
        String typeName = inputSchema.getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputSchema, typeName);

        Class<?> fieldBinding = Double.class; // default
        if (FeatureTypes.existProeprty(inputSchema, valueField)) {
            fieldBinding = featureType.getDescriptor(valueField).getType().getBinding();
        } else {
            featureType = FeatureTypes.add(featureType, valueField, Double.class);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        final double noData = RasterHelper.getNoDataValue(surfaceRaster);
        RasterFunctionalSurface surface = new RasterFunctionalSurface(surfaceRaster);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                // do process
                double val = noData;
                switch (valueType) {
                case Default:
                    val = surface.getElevation(geometry.getCentroid());
                    break;
                case SlopeAsDegree:
                    val = surface.getSlope(geometry.getCentroid(), SlopeType.Degree);
                    break;
                case SlopeAsPercentrise:
                    val = surface.getSlope(geometry.getCentroid(), SlopeType.Percentrise);
                    break;
                case Aspect:
                    val = surface.getAspect(geometry.getCentroid());
                    break;
                }

                // copy feature and set value
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
                newFeature.setAttribute(valueField, Converters.convert(val, fieldBinding));

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

}
