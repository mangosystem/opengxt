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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.enumeration.PointAssignmentType;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Converts features to a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeaturesToRasterOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(FeaturesToRasterOperation.class);

    private FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

    private DiskMemImage dmImage = null;

    private Graphics2D g2D = null;

    private AffineTransform affineTrans = null;

    private SimpleShapeType shapeType = SimpleShapeType.POINT;

    public GridCoverage2D execute(SimpleFeatureCollection inputFeatures, int gridValue) {
        return execute(inputFeatures, Integer.valueOf(gridValue));
    }

    public GridCoverage2D execute(SimpleFeatureCollection inputFeatures, Number gridValue) {
        shapeType = FeatureTypes.getSimpleShapeType(inputFeatures.getSchema());

        RasterPixelType rsType = RasterPixelType.SHORT;
        if (gridValue.getClass().equals(Short.class)) {
            rsType = RasterPixelType.SHORT;
        } else if (gridValue.getClass().equals(Integer.class)) {
            rsType = RasterPixelType.INTEGER;
        } else if (gridValue.getClass().equals(Float.class)) {
            rsType = RasterPixelType.FLOAT;
        } else if (gridValue.getClass().equals(Double.class)) {
            rsType = RasterPixelType.DOUBLE;
        } else if (gridValue.getClass().equals(Byte.class)) {
            rsType = RasterPixelType.BYTE;
        }

        // calculate extent & cellsize
        Object nodataValue = RasterHelper.getDefaultNoDataValue(rsType);
        calculateExtentAndCellSize(inputFeatures.getBounds(), nodataValue);

        // Point To Raster Conversion
        if (shapeType == SimpleShapeType.POINT) {
            PointsToRasterOperation pointToRaster = new PointsToRasterOperation();
            pointToRaster.setExtentAndCellSize(gridExtent, pixelSizeX, pixelSizeY);

            return pointToRaster.execute(inputFeatures, gridValue.intValue());
        }

        initializeTiledImage(gridExtent, rsType);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                processGeometry(geometry, gridValue);
            }
        } finally {
            featureIter.close();
        }

        this.minValue = this.maxValue = gridValue.doubleValue();

        return close();
    }

    public GridCoverage2D execute(SimpleFeatureCollection inputFeatures) {
        // use internal id
        return execute(inputFeatures, "");
    }

    private Map<Object, Integer> getUniqueValues(SimpleFeatureCollection inputFeatures,
            String valueField) {
        Map<Object, Integer> uvMap = new TreeMap<Object, Integer>();

        Expression valueExp = ff.property(valueField);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                String value = valueExp.evaluate(feature, String.class);
                if (value == null) {
                    value = "";
                }
                uvMap.put(value, Integer.valueOf(0));
            }
        } finally {
            featureIter.close();
        }

        // renumber
        int gridValue = 1;
        for (Entry<Object, Integer> entrySet : uvMap.entrySet()) {
            entrySet.setValue(Integer.valueOf(gridValue++));
        }

        return uvMap;
    }

    public GridCoverage2D execute(SimpleFeatureCollection inputFeatures, String valueField) {
        SimpleFeatureType schema = inputFeatures.getSchema();
        Class<?> geomBinding = schema.getGeometryDescriptor().getType().getBinding();
        shapeType = FeatureTypes.getSimpleShapeType(geomBinding);
        RasterPixelType transferType = RasterPixelType.SHORT;

        // calculate extent & cellsize
        Object nodataValue = RasterHelper.getDefaultNoDataValue(transferType);
        calculateExtentAndCellSize(inputFeatures.getBounds(), nodataValue);

        // Point To Raster Conversion
        if (shapeType == SimpleShapeType.POINT) {
            PointsToRasterOperation pointToRaster = new PointsToRasterOperation();
            pointToRaster.setExtentAndCellSize(gridExtent, pixelSizeX, pixelSizeY);

            return pointToRaster.execute(inputFeatures, valueField,
                    PointAssignmentType.MostFrequent);
        }

        // Polygon or LineString
        boolean isNuemricField = false;
        boolean useFID = false;
        if (StringHelper.isNullOrEmpty(valueField)) {
            useFID = true;
        } else {
            valueField = FeatureTypes.validateProperty(schema, valueField);
            if (schema.indexOf(valueField) == -1) {
                useFID = true;
            } else {
                isNuemricField = FeatureTypes.isNumeric(schema, valueField);
            }
        }

        Map<Object, Integer> uvMap = new TreeMap<Object, Integer>();
        if (isNuemricField) {
            transferType = RasterHelper.getTransferType(schema, valueField);
        } else {
            if (!useFID) {
                uvMap = getUniqueValues(inputFeatures, valueField);
            }
        }

        initializeTiledImage(gridExtent, transferType);

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            Expression valueExp = ff.property(valueField);
            int featureID = 1;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                if (isNuemricField) {
                    Number gridValue = valueExp.evaluate(feature, Number.class);
                    if (gridValue == null) {
                        gridValue = this.noData;
                    }

                    processGeometry(geometry, gridValue);
                } else if (useFID) {
                    processGeometry(geometry, featureID++);
                } else {
                    String value = valueExp.evaluate(feature, String.class);
                    if (value == null) {
                        value = "";
                    }

                    processGeometry(geometry, uvMap.get(value));
                }
            }
        } finally {
            featureIter.close();
        }

        return close();
    }

    private void initializeTiledImage(ReferencedEnvelope gridExtent, RasterPixelType transferType) {
        // set pixel type
        pixelType = transferType;

        final int tw = 64;
        final int th = 64;

        ColorModel colorModel = ColorModel.getRGBdefault();
        SampleModel smpModel = colorModel.createCompatibleSampleModel(tw, th);

        Dimension dim = RasterHelper.getDimension(gridExtent, pixelSizeX, pixelSizeY);

        dmImage = new DiskMemImage(0, 0, dim.width, dim.height, 0, 0, smpModel, colorModel);
        dmImage.setUseCommonCache(true);

        // create graphics
        g2D = dmImage.createGraphics();

        g2D.setPaintMode();
        g2D.setComposite(AlphaComposite.Src);

        // set nodata value
        g2D.setPaint(valueToColor(noData));
        g2D.fillRect(0, 0, dmImage.getWidth(), dmImage.getHeight());

        if (shapeType == SimpleShapeType.LINESTRING) {
            g2D.setStroke(
                    new BasicStroke(1.1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f));
        }

        // setup affine transform
        double x_scale = dmImage.getWidth() / gridExtent.getWidth();
        double y_scale = dmImage.getHeight() / gridExtent.getHeight();
        Coordinate centerPos = gridExtent.centre();

        affineTrans = new AffineTransform();
        affineTrans.translate(dmImage.getWidth() / 2.0, dmImage.getHeight() / 2.0);
        affineTrans.scale(x_scale, -y_scale);
        affineTrans.translate(-centerPos.x, -centerPos.y);
    }

    private void processGeometry(Geometry geometry, Number value) {
        if (!gridExtent.intersects(geometry.getEnvelopeInternal())) {
            return;
        }

        // update statistics
        updateStatistics(value.doubleValue());

        g2D.setPaint(valueToColor(value));

        int numGeom = geometry.getNumGeometries();
        for (int i = 0; i < numGeom; i++) {
            Geometry geomN = geometry.getGeometryN(i);
            drawSingleGeometry(geomN);
        }
    }

    private void drawSingleGeometry(Geometry srcGeom) {
        if (srcGeom.getClass().equals(Polygon.class)) {
            Polygon polygon = (Polygon) srcGeom;
            GeneralPath path = toGeneralPath(polygon);
            path.transform(affineTrans);
            g2D.fill(path);
        } else if (srcGeom.getClass().equals(LineString.class)) {
            LineString lineString = (LineString) srcGeom;
            GeneralPath path = toGeneralPath(lineString);
            path.transform(affineTrans);
            g2D.draw(path);
        } else if (srcGeom.getClass().equals(Point.class)) {
            Coordinate c = srcGeom.getCoordinate();

            Point2D.Double ptDst = new Point2D.Double();
            affineTrans.transform(new Point2D.Double(c.x, c.y), ptDst);
            Rectangle2D.Double square = new Rectangle2D.Double(ptDst.x, ptDst.y, 1, 1);
            g2D.fill(square);
        }
    }

    private GeneralPath toGeneralPath(Geometry srcGeometry) {
        GeneralPath gPath = new GeneralPath();

        if (srcGeometry.getClass().isAssignableFrom(LineString.class)) {
            addLineStringToPath(false, (LineString) srcGeometry, gPath);
        } else if (srcGeometry.getClass().isAssignableFrom(Polygon.class)) {
            Polygon polygon = (Polygon) srcGeometry;

            LineString exterior = polygon.getExteriorRing();
            addLineStringToPath(true, exterior, gPath);

            int numInterior = polygon.getNumInteriorRing();
            for (int j = 0; j < numInterior; j++) {
                LineString interior = polygon.getInteriorRingN(j);
                addLineStringToPath(true, interior, gPath);
            }
        }

        return gPath;
    }

    private void addLineStringToPath(boolean isRing, LineString lineString,
            GeneralPath targetPath) {
        Coordinate[] cs = lineString.getCoordinates();

        double xOffset = 0;
        double yOffset = 0;

        // Offset like ArcGIS
        if (shapeType == SimpleShapeType.POLYGON) {
            xOffset = pixelSizeX / 2.0;
            yOffset = pixelSizeY / 2.0;
        }

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

    private GridCoverage2D close() {
        g2D.dispose();

        SampleModel sm = null;
        ColorSpace scs = null;
        ColorModel cm = null;
        DiskMemImage destImage = null;

        switch (pixelType) {
        case BYTE:
        case SHORT:
            sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_SHORT,
                    dmImage.getTileWidth(), dmImage.getTileHeight(), 1);
            scs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            cm = new ComponentColorModel(scs, false, false, Transparency.TRANSLUCENT,
                    DataBuffer.TYPE_SHORT);
            break;
        case INTEGER:
            sm = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_INT,
                    dmImage.getTileWidth(), dmImage.getTileHeight(), 1);
            scs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            cm = new ComponentColorModel(scs, false, false, Transparency.OPAQUE,
                    DataBuffer.TYPE_INT);
            break;
        case FLOAT:
        case DOUBLE:
            sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT,
                    dmImage.getTileWidth(), dmImage.getTileHeight(), 1);
            cm = PlanarImage.createColorModel(sm);
            break;
        }

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
                        switch (pixelType) {
                        case BYTE:
                        case SHORT:
                        case INTEGER:
                            destTile.setSample(dx, dy, 0, data[k++]);
                            break;
                        case FLOAT:
                            destTile.setSample(dx, dy, 0, Float.intBitsToFloat(data[k++]));
                            break;
                        case DOUBLE:
                            destTile.setSample(dx, dy, 0, Double.longBitsToDouble(data[k++]));
                            break;
                        }
                    }
                }

                destImage.releaseWritableTile(tileX, tileY);
            }
        }

        return createGridCoverage("FeaturesToRaster", destImage);
    }

    private Color valueToColor(Number value) {
        int intBits;

        switch (pixelType) {
        case BYTE:
            intBits = value.byteValue();
            break;
        case SHORT:
            intBits = value.shortValue();
            break;
        case INTEGER:
            intBits = value.intValue();
            break;
        case FLOAT:
            intBits = Float.floatToIntBits(value.floatValue());
            break;
        case DOUBLE:
            intBits = Float.floatToIntBits(value.floatValue());
            break;
        default:
            intBits = value.intValue();
            break;
        }

        return new Color(intBits, true);
    }

}