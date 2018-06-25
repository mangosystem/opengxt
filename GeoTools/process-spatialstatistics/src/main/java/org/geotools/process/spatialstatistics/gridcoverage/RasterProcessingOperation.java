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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.UnpackedImageData;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.storage.FeatureInserter;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NullProgressListener;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Point;

/**
 * Abstract Raster Processing Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterProcessingOperation.class);

    private RasterEnvironment rasterEnvironment = new RasterEnvironment();

    public void setRasterEnvironment(RasterEnvironment rasterEnvironment) {
        this.rasterEnvironment = rasterEnvironment;
    }

    public RasterEnvironment getRasterEnvironment() {
        return rasterEnvironment;
    }

    public ProgressListener Progress = new NullProgressListener();
    
    protected int MIN_CELL_COUNT = 600;

    // it is the shorter of the width or the height of the extent of the input point features
    // in the input spatial reference, divided by 250.

    protected double CellSize = 30.0d;

    protected ReferencedEnvelope Extent = null;

    protected RasterPixelType PixelType = RasterPixelType.INTEGER;

    protected double NoData = Float.MIN_VALUE;

    protected double MinValue = Double.MAX_VALUE;

    protected double MaxValue = Double.MIN_VALUE;

    private String outTypeName = null;

    private DataStore outDataStore = null;

    public void setOutputDataStore(DataStore outputDataStore) {
        this.outDataStore = outputDataStore;
    }

    public DataStore getOutputDataStore() {
        if (outDataStore == null) {
            outDataStore = new MemoryDataStore();
        }
        return outDataStore;
    }

    public void setOuptputTypeName(String ouptputTypeName) {
        this.outTypeName = ouptputTypeName;
    }

    public String getOuptputTypeName() {
        if (outTypeName == null) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd_hhmmss_S");
            String serialID = dataFormat.format(Calendar.getInstance().getTime());
            outTypeName = getClass().getSimpleName().toLowerCase() + "_" + serialID;
        }

        return outTypeName;
    }

    public GridCoverage2D saveAsGeoTiff(GridCoverage2D inputCoverage, String geoTifFile) {
        RasterExportOperation saveAsOp = new RasterExportOperation();
        try {
            return saveAsOp.saveAsGeoTiff(inputCoverage, geoTifFile);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    protected void updateStatistics(double retVal) {
        if (SSUtils.compareDouble(retVal, NoData)) {
            return;
        }

        this.MinValue = Math.min(MinValue, retVal);
        this.MaxValue = Math.max(MaxValue, retVal);
    }

    /**
     * Fill the specified raster with the provided background values
     * 
     * @param writableImage WritableRenderedImage like DiskMemImage
     * @param initValue Background value
     */
    protected void initializeDefaultValue(DiskMemImage writableImage, Double initValue) {
        final int numBands = writableImage.getSampleModel().getNumBands();
        final double[] backgroundValues = new double[numBands];
        for (int index = 0; index < numBands; index++) {
            backgroundValues[index] = initValue;
        }

        // raw level
        WritableRaster raster = (WritableRaster) writableImage.getData();
        // ImageUtil.fillBackground(raster, raster.getBounds(), backgroundValues);
        this.fillBackground(raster, raster.getBounds(), backgroundValues);
        writableImage.setData(raster);
    }

    private boolean isBinary(SampleModel sm) {
        return sm instanceof MultiPixelPackedSampleModel
                && ((MultiPixelPackedSampleModel) sm).getPixelBitStride() == 1
                && sm.getNumBands() == 1;
    }

    /**
     * Fill the specified rectangle of <code>raster</code> with the provided background values. Suppose the raster is initialized to 0. Thus, for
     * binary data, if the provided background values are 0, do nothing.
     */
    private void fillBackground(WritableRaster raster, Rectangle rect, double[] backgroundValues) {
        rect = rect.intersection(raster.getBounds());
        SampleModel sm = raster.getSampleModel();
        PixelAccessor accessor = new PixelAccessor(sm, null);
        if (isBinary(sm)) {
            // fill binary data
            byte value = (byte) (((int) backgroundValues[0]) & 1);
            if (value == 0)
                return;
            int rectX = rect.x;
            int rectY = rect.y;
            int rectWidth = rect.width;
            int rectHeight = rect.height;
            int dx = rectX - raster.getSampleModelTranslateX();
            int dy = rectY - raster.getSampleModelTranslateY();
            DataBuffer dataBuffer = raster.getDataBuffer();
            MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel) sm;
            int lineStride = mpp.getScanlineStride();
            int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
            int bitOffset = mpp.getBitOffset(dx);
            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE: {
                byte[] data = ((DataBufferByte) dataBuffer).getData();
                int bits = bitOffset & 7;
                int otherBits = (bits == 0) ? 0 : 8 - bits;
                byte mask = (byte) (255 >> bits);
                int lineLength = (rectWidth - otherBits) / 8;
                int bits1 = (rectWidth - otherBits) & 7;
                byte mask1 = (byte) (255 << (8 - bits1));
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }
                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = (byte) 255;
                    if (bits1 != 0)
                        data[start] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                short[] data = ((DataBufferUShort) dataBuffer).getData();
                int bits = bitOffset & 15;
                int otherBits = (bits == 0) ? 0 : 16 - bits;
                short mask = (short) (65535 >> bits);
                int lineLength = (rectWidth - otherBits) / 16;
                int bits1 = (rectWidth - otherBits) & 15;
                short mask1 = (short) (65535 << (16 - bits1));
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }
                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = (short) 0xFFFF;
                    if (bits1 != 0)
                        data[start++] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }
            case DataBuffer.TYPE_INT: {
                int[] data = ((DataBufferInt) dataBuffer).getData();
                int bits = bitOffset & 31;
                int otherBits = (bits == 0) ? 0 : 32 - bits;
                int mask = 0xFFFFFFFF >> bits;
                int lineLength = (rectWidth - otherBits) / 32;
                int bits1 = (rectWidth - otherBits) & 31;
                int mask1 = 0xFFFFFFFF << (32 - bits1);
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }
                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = 0xFFFFFFFF;
                    if (bits1 != 0)
                        data[start++] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }
            }
        } else {
            int srcSampleType = accessor.sampleType == PixelAccessor.TYPE_BIT ? DataBuffer.TYPE_BYTE
                    : accessor.sampleType;
            UnpackedImageData uid = accessor.getPixels(raster, rect, srcSampleType, false);
            rect = uid.rect;
            int lineStride = uid.lineStride;
            int pixelStride = uid.pixelStride;
            switch (uid.type) {
            case DataBuffer.TYPE_BYTE:
                byte[][] bdata = uid.getByteData();
                for (int b = 0; b < accessor.numBands; b++) {
                    byte value = (byte) backgroundValues[b];
                    byte[] bd = bdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;
                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            bd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[][] sdata = uid.getShortData();
                for (int b = 0; b < accessor.numBands; b++) {
                    short value = (short) backgroundValues[b];
                    short[] sd = sdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;
                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            sd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_INT:
                int[][] idata = uid.getIntData();
                for (int b = 0; b < accessor.numBands; b++) {
                    int value = (int) backgroundValues[b];
                    int[] id = idata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;
                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            id[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[][] fdata = uid.getFloatData();
                for (int b = 0; b < accessor.numBands; b++) {
                    float value = (float) backgroundValues[b];
                    float[] fd = fdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;
                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            fd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[][] ddata = uid.getDoubleData();
                for (int b = 0; b < accessor.numBands; b++) {
                    double value = backgroundValues[b];
                    double[] dd = ddata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;
                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            dd[po] = value;
                        }
                    }
                }
                break;
            }
        }
    }

    protected void calculateExtentAndCellSize(SimpleFeatureCollection srcSfs, Object noDataValue) {
        calculateExtentAndCellSize(srcSfs.getBounds(), noDataValue);
    }

    protected void calculateExtentAndCellSize(ReferencedEnvelope srcExtent, Object noDataValue) {
        // calculate extent & cellsize
        CellSize = this.getRasterEnvironment().getCellSize();
        NoData = Double.parseDouble(noDataValue.toString());
        Extent = this.getRasterEnvironment().getExtent();

        boolean boundUpdated = false;
        if (Extent == null) {
            Extent = srcExtent;
            boundUpdated = true;
        }

        if (Double.isNaN(CellSize)) {
            // it is the shorter of the width or the height of the extent of the input point
            // features in the input spatial reference, divided by 250.
            this.CellSize = Math.min(Extent.getWidth(), Extent.getHeight()) / 250.0;
        }

        if (boundUpdated) {
            Extent.expandBy(CellSize / 2.0);
        }
    }

    protected DiskMemImage createDiskMemImage(GridCoverage2D srcCoverage,
            RasterPixelType transferType) {
        Extent = new ReferencedEnvelope(srcCoverage.getEnvelope());
        CellSize = RasterHelper.getCellSize(srcCoverage);

        final RenderedImage img = srcCoverage.getRenderedImage();
        return createDiskMemImage(Extent, transferType, img.getTileWidth(), img.getTileHeight());
    }

    protected DiskMemImage createDiskMemImage(ReferencedEnvelope extent,
            RasterPixelType transferType) {
        final int tw = 64;
        final int th = 64;

        return createDiskMemImage(extent, transferType, tw, th);
    }

    protected DiskMemImage createDiskMemImage(ReferencedEnvelope extent,
            RasterPixelType transferType, int tw, int th) {
        // set pixel type
        PixelType = transferType;

        // recalculate coverage extent
        Extent = RasterHelper.getResolvedEnvelope(extent, CellSize);

        // initialize statistics
        MinValue = Double.MAX_VALUE;
        MaxValue = Double.MIN_VALUE;

        // We need a sample model. The most appropriate is created as shown:
        SampleModel sampleModel = null;
        ColorModel cm = null;

        switch (transferType) {
        case BYTE:
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, tw, th, 1);
            ColorSpace bcs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            cm = new ComponentColorModel(bcs, false, false, Transparency.TRANSLUCENT,
                    DataBuffer.TYPE_BYTE);
            break;
        case SHORT:
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_SHORT, tw, th, 1);
            ColorSpace scs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            cm = new ComponentColorModel(scs, false, false, Transparency.TRANSLUCENT,
                    DataBuffer.TYPE_SHORT);
            break;
        case INTEGER:
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_INT, tw, th, 1);
            cm = PlanarImage.createColorModel(sampleModel);
            break;
        case FLOAT:
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, tw, th, 1);
            cm = PlanarImage.createColorModel(sampleModel);
            break;
        case DOUBLE:
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, tw, th, 1);
            cm = PlanarImage.createColorModel(sampleModel);
            break;
        }

        // Create a TiledImage using the SampleModel.
        Dimension dm = RasterHelper.getDimension(Extent, CellSize);

        DiskMemImage diskMemImage = null;
        diskMemImage = new DiskMemImage(0, 0, dm.width, dm.height, 0, 0, sampleModel, cm);
        diskMemImage.setUseCommonCache(true);

        // Get a raster for the single tile.
        return diskMemImage;
    }

    protected IFeatureInserter getTransactionFeatureStore(SimpleFeatureType featureType) {
        // create feature store
        SimpleFeatureStore featureStore = null;

        try {
            getOutputDataStore().createSchema(featureType);

            String typeName = featureType.getTypeName();
            SimpleFeatureSource sfs = getOutputDataStore().getFeatureSource(typeName);

            if (sfs instanceof SimpleFeatureStore) {
                featureStore = (SimpleFeatureStore) sfs;
                Transaction transaction = new DefaultTransaction(typeName);
                featureStore.setTransaction(transaction);
            } else {
                LOGGER.log(Level.FINE, sfs.getName().toString()
                        + " does not support SimpleFeatureStore interface!");
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }

        return new FeatureInserter(featureStore);
    }

    protected GridCoverage2D createGridCoverage(CharSequence name, RenderedImage image,
            GridSampleDimension[] bands, double noDataValue, double minValue, double maxValue,
            ReferencedEnvelope extent) {

        if (image == null || extent == null) {
            throw new NullPointerException("WritableRaster is null!");
        }

        if (noDataValue == minValue) {
            noDataValue = minValue - 1;
        } else if (noDataValue == maxValue) {
            noDataValue = maxValue + 1;
        } else if (noDataValue > minValue && noDataValue < maxValue) {
            noDataValue = minValue - 1;
        }

        CharSequence noDataName = Vocabulary.formatInternational(VocabularyKeys.NODATA);

        // setting metadata
        final Map<CharSequence, Double> properties = new HashMap<CharSequence, Double>();
        properties.put("Maximum", Double.valueOf(maxValue));
        properties.put("Minimum", Double.valueOf(minValue));
        // properties.put("Mean", 1.0);
        // properties.put("StdDev", 1.0);
        properties.put(noDataName, Double.valueOf(noDataValue));
        properties.put("GC_NODATA", Double.valueOf(noDataValue));

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        return factory.create(name, image, extent, bands, null, properties);
    }

    protected GridCoverage2D createGridCoverage(CharSequence name, PlanarImage tiledImage) {
        return createGridCoverage(name, tiledImage, 1, NoData, MinValue, MaxValue, Extent);
    }

    protected GridCoverage2D createGridCoverage(CharSequence name, PlanarImage tiledImage,
            int bandCount, double noDataValue, double minValue, double maxValue,
            ReferencedEnvelope extent) {

        if (tiledImage == null || extent == null) {
            throw new NullPointerException("WritableRaster is null!");
        }

        if (noDataValue == minValue) {
            noDataValue = minValue - 1;
        } else if (noDataValue == maxValue) {
            noDataValue = maxValue + 1;
        } else if (noDataValue > minValue && noDataValue < maxValue) {
            noDataValue = minValue - 1;
        }

        CharSequence noDataName = Vocabulary.formatInternational(VocabularyKeys.NODATA);
        Category nan = new Category(noDataName, new Color[] { new Color(255, 255, 255, 0) },
                NumberRange.create(noDataValue, noDataValue));

        Color[] colors = new Color[] { Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED };
        Category values = new Category("values", colors, NumberRange.create(minValue, maxValue));

        GridSampleDimension band = new GridSampleDimension("Dimension", new Category[] { nan,
                values }, null);
        GridSampleDimension[] bands = new GridSampleDimension[] { band };

        // setting metadata
        final Map<CharSequence, Double> properties = new HashMap<CharSequence, Double>();
        properties.put("Maximum", Double.valueOf(maxValue));
        properties.put("Minimum", Double.valueOf(minValue));
        // properties.put("Mean", 1.0);
        // properties.put("StdDev", 1.0);
        properties.put(noDataName, Double.valueOf(noDataValue));
        properties.put("GC_NODATA", Double.valueOf(noDataValue));

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        return factory.create(name, tiledImage, extent, bands, null, properties);
    }

    protected SimpleFeature createTemplateFeature(GridCoverage2D inputGc) {
        String typeName = inputGc.getName().toString();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(typeName, Point.class,
                inputGc.getCoordinateReferenceSystem());
        schema = FeatureTypes.add(schema, typeName, Double.class);
        schema = FeatureTypes.add(schema, "Value", Double.class);
        return new SimpleFeatureBuilder(schema).buildFeature(null);
    }
}
