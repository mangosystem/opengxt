/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.util.logging.Logger;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.RasterFactory;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.DiskMemImage;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Calculate the density of input features within a neighborhood around each output raster cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterDensityOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterDensityOperation.class);

    protected double scaleArea = 0.0;

    protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

    protected Filter getBBoxFilter(SimpleFeatureType schema, ReferencedEnvelope extent,
            double expandDistance) {
        String the_geom = schema.getGeometryDescriptor().getLocalName();

        ReferencedEnvelope bbox = new ReferencedEnvelope(extent);
        if (expandDistance != 0) {
            bbox.expandBy(expandDistance);
        }

        return ff.bbox(ff.property(the_geom), bbox);
    }

    protected PlanarImage scaleUnit(PlanarImage image) {
        if (scaleArea == 0.0) {
            return image;
        }

        // Copy data and divide each sample by the scale factor
        WritableRaster raster = image.getData().createCompatibleWritableRaster();
        image.copyData(raster);

        final int width = raster.getWidth();
        final int height = raster.getHeight();
        final int numBands = raster.getNumBands();

        double[] pixel = new double[numBands];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.getPixel(x, y, pixel);
                for (int b = 0; b < numBands; b++) {
                    pixel[b] = pixel[b] / scaleArea;
                }
                raster.setPixel(x, y, pixel);
            }
        }

        // Rebuild a DiskMemImage with the divided data
        ColorModel cm = image.getColorModel();
        if (cm == null) {
            cm = PlanarImage.createColorModel(image.getSampleModel());
        }

        SampleModel sm = RasterFactory.createBandedSampleModel(raster.getDataBuffer().getDataType(),
                image.getTileWidth(), image.getTileHeight(), numBands);

        DiskMemImage out = new DiskMemImage(0, 0, width, height, 0, 0, sm, cm);
        out.setData(raster);
        return out;
    }

    protected PlanarImage pointToRaster(SimpleFeatureCollection pointFeatures,
            String populationField) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Integer.MIN_VALUE);

        if (!StringHelper.isNullOrEmpty(populationField)) {
            populationField = FeatureTypes.validateProperty(pointFeatures.getSchema(),
                    populationField);
        }

        DiskMemImage outputImage = this.createDiskMemImage(gridExtent, RasterPixelType.FLOAT);
        this.initializeDefaultValue(outputImage, 0.0);

        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), gridExtent);

        GridTransformer trans = new GridTransformer(gridExtent, pixelSizeX, pixelSizeY);
        SimpleFeatureIterator featureIter = pointFeatures.subCollection(filter).features();
        try {
            Expression weightExp = ff.property(populationField);
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();

                // Multipoints are treated as a set of individual points.
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                for (int iPart = 0; iPart < multiPoint.getNumGeometries(); iPart++) {
                    final Coordinate realPos = multiPoint.getGeometryN(iPart).getCoordinate();
                    final GridCoordinates2D gridPos = trans.worldToGrid(realPos);
                    if (trans.contains(gridPos.x, gridPos.y)) {
                        final Double dblVal = weightExp.evaluate(feature, Double.class);
                        double wVal = dblVal == null ? 1.0 : dblVal.doubleValue();

                        wVal += outputImage.getSampleDouble(gridPos.x, gridPos.y, 0);

                        outputImage.setSample(gridPos.x, gridPos.y, 0, wVal);
                        this.maxValue = Math.max(maxValue, wVal);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        return outputImage;
    }
}
