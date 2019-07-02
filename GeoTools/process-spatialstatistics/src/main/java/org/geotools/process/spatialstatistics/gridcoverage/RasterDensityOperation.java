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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

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

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    protected PlanarImage scaleUnit(PlanarImage image) {
        final RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        final ParameterBlockJAI pb = new ParameterBlockJAI("DivideByConst",
                RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", image);
        pb.setParameter("constants", new double[] { scaleArea });

        return JAI.create("DivideByConst", pb, hints);
    }

    protected PlanarImage pointToRaster(SimpleFeatureCollection pointFeatures,
            String populationField) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Integer.MIN_VALUE);

        if (!StringHelper.isNullOrEmpty(populationField)) {
            populationField = FeatureTypes.validateProperty(pointFeatures.getSchema(),
                    populationField);
        }

        DiskMemImage outputImage = this.createDiskMemImage(Extent, RasterPixelType.FLOAT);
        this.initializeDefaultValue(outputImage, 0.0);

        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), Extent);

        GridTransformer trans = new GridTransformer(Extent, CellSizeX, CellSizeY);
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
                        this.MaxValue = Math.max(MaxValue, wVal);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        return outputImage;
    }

    protected PlanarImage lineToRaster(SimpleFeatureCollection lineFeatures, String populationField) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(lineFeatures, Integer.MIN_VALUE);
        Extent = RasterHelper.getResolvedEnvelope(Extent, CellSizeX, CellSizeY);

        Expression valueExp = ff.literal(1.0); // default
        if (!StringHelper.isNullOrEmpty(populationField)) {
            populationField = FeatureTypes.validateProperty(lineFeatures.getSchema(),
                    populationField);
            valueExp = ff.property(populationField);
        }

        DiskMemImage dmImage;
        ColorModel colorModel = ColorModel.getRGBdefault();
        SampleModel smpModel = colorModel.createCompatibleSampleModel(64, 64);
        Dimension dim = RasterHelper.getDimension(Extent, CellSizeX, CellSizeY);
        dmImage = new DiskMemImage(0, 0, dim.width, dim.height, 0, 0, smpModel, colorModel);

        dmImage.setUseCommonCache(true);

        // create graphics
        Graphics2D g2D = dmImage.createGraphics();
        g2D.setPaintMode();
        g2D.setComposite(AlphaComposite.Src);

        // set background value to zero
        g2D.setPaint(new Color(Float.floatToIntBits(0.0f), true));
        g2D.fillRect(0, 0, dmImage.getWidth(), dmImage.getHeight());
        g2D.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f));

        g2D.setComposite(BlendAddComposite.getInstance());

        // setup affine transform
        double x_scale = dmImage.getWidth() / Extent.getWidth();
        double y_scale = dmImage.getHeight() / Extent.getHeight();
        Coordinate centerPos = Extent.centre();

        AffineTransform affineTrans = new AffineTransform();
        affineTrans.translate(dmImage.getWidth() / 2.0, dmImage.getHeight() / 2.0);
        affineTrans.scale(x_scale, -y_scale);
        affineTrans.translate(-centerPos.x, -centerPos.y);

        // draw line
        SimpleFeatureIterator featureIter = lineFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Number gridValue = valueExp.evaluate(feature, Double.class);
                if (gridValue == null) {
                    gridValue = 0.0; // nodata to zero
                }

                int intBits = Float.floatToIntBits(gridValue.floatValue());
                g2D.setPaint(new Color(intBits, true));

                GeneralPath path = new GeneralPath();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {
                    LineString lineString = (LineString) geometry.getGeometryN(i);

                    path.reset();
                    addLineStringToPath(false, lineString, path);
                    path.transform(affineTrans);

                    g2D.draw(path);
                }
            }
        } finally {
            featureIter.close();
        }

        g2D.dispose();

        // finally create image
        DiskMemImage destImage;
        SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT,
                dmImage.getTileWidth(), dmImage.getTileHeight(), 1);
        ColorModel cm = PlanarImage.createColorModel(sm);
        destImage = new DiskMemImage(0, 0, dmImage.getWidth(), dmImage.getHeight(), 0, 0, sm, cm);
        destImage.setUseCommonCache(true);

        int numXTiles = dmImage.getNumXTiles();
        int numYTiles = dmImage.getNumYTiles();
        for (int tileY = 0; tileY < numYTiles; tileY++) {
            for (int tileX = 0; tileX < numXTiles; tileX++) {
                Raster srcTile = dmImage.getTile(tileX, tileY);
                WritableRaster destTile = destImage.getWritableTile(tileX, tileY);

                final int[] data = new int[srcTile.getDataBuffer().getSize()];
                srcTile.getDataElements(srcTile.getMinX(), srcTile.getMinY(), srcTile.getWidth(),
                        srcTile.getHeight(), data);

                int k = 0;
                final Rectangle bounds = destTile.getBounds();
                for (int dy = bounds.y, drow = 0; drow < bounds.height; dy++, drow++) {
                    for (int dx = bounds.x, dcol = 0; dcol < bounds.width; dx++, dcol++) {
                        float wVal = Float.intBitsToFloat(data[k++]);
                        destTile.setSample(dx, dy, 0, wVal);
                        this.MaxValue = Math.max(MaxValue, wVal);
                    }
                }

                destImage.releaseWritableTile(tileX, tileY);
            }
        }

        return destImage;
    }

    private void addLineStringToPath(boolean isRing, LineString lineString, GeneralPath targetPath) {
        Coordinate[] cs = lineString.getCoordinates();

        double xOffset = 0;
        double yOffset = 0;

        for (int i = 0; i < cs.length; i++) {
            if (i == 0) {
                targetPath.moveTo(cs[i].x - xOffset, cs[i].y + yOffset);
            } else {
                targetPath.lineTo(cs[i].x - xOffset, cs[i].y + yOffset);
            }
        }

        if (isRing && !lineString.isClosed()) {
            targetPath.closePath();
        }
    }

    static final class BlendAddComposite implements Composite {

        private final float alpha;

        private BlendAddComposite() {
            this(1.0f);
        }

        private BlendAddComposite(float alpha) {
            this.alpha = Math.max(Math.min(alpha, 1.0f), 0.0f);
        }

        public static BlendAddComposite getInstance() {
            return new BlendAddComposite();
        }

        @Override
        public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
                RenderingHints hints) {

            return new CompositeContext() {
                @Override
                public void dispose() {

                }

                @Override
                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int width = Math.min(src.getWidth(), dstIn.getWidth());
                    int height = Math.min(src.getHeight(), dstIn.getHeight());

                    int[] srcPixels = new int[width];
                    int[] dstPixels = new int[width];

                    for (int y = 0; y < height; y++) {
                        src.getDataElements(0, y, width, 1, srcPixels);
                        dstIn.getDataElements(0, y, width, 1, dstPixels);
                        for (int x = 0; x < width; x++) {
                            float srcVal = Float.intBitsToFloat(srcPixels[x]);
                            float dstVal = Float.intBitsToFloat(dstPixels[x]);

                            int pixel = dstPixels[x];
                            int dr = (pixel >> 16) & 0xFF;
                            int dg = (pixel >> 8) & 0xFF;
                            int db = (pixel) & 0xFF;
                            int da = (pixel >> 24) & 0xFF;

                            // Add
                            pixel = Float.floatToIntBits(srcVal + dstVal);
                            int or = (pixel >> 16) & 0xFF;
                            int og = (pixel >> 8) & 0xFF;
                            int ob = (pixel) & 0xFF;
                            int oa = (pixel >> 24) & 0xFF;

                            // mixes the result with the alpha blending
                            or = (int) (dr + (or - dr) * alpha);
                            og = (int) (dg + (og - dg) * alpha);
                            ob = (int) (db + (ob - db) * alpha);
                            oa = (int) (da + (oa - da) * alpha);

                            dstPixels[x] = oa << 24 | or << 16 | og << 8 | ob & 0xFF;
                        }
                        dstOut.setDataElements(0, y, width, 1, dstPixels);
                    }
                }
            };
        }
    }
}
