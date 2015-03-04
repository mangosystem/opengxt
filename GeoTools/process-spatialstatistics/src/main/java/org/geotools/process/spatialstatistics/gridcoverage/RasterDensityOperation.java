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

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.registry.RenderedRegistryMode;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Calculate the density of input features within a neighborhood around each output raster cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterDensityOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterKernelDensityOperation.class);

    double scaleArea = 0.0;

    protected PlanarImage scaleUnit(PlanarImage source0) {
        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        final ParameterBlockJAI pb = new ParameterBlockJAI("DivideByConst",
                RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setParameter("constants", new double[] { scaleArea });

        return JAI.create("DivideByConst", pb, hints);
    }

    protected PlanarImage pointToRaster(SimpleFeatureCollection pointFeatures, String weightField) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Integer.MIN_VALUE);

        if (!StringHelper.isNullOrEmpty(weightField)) {
            weightField = FeatureTypes.validateProperty(pointFeatures.getSchema(), weightField);
        }

        DiskMemImage outputImage = this.createDiskMemImage(Extent, RasterPixelType.FLOAT);
        this.initializeDefaultValue(outputImage, 0.0);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Expression weightExp = ff.property(weightField);

        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), Extent);

        GridTransformer trans = new GridTransformer(Extent, CellSize);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = pointFeatures.subCollection(filter).features();
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
                        updateStatistics(wVal);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        return outputImage;
    }

}
