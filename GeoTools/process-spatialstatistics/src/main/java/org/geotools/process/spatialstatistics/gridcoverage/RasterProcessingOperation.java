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
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
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

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.storage.FeatureInserter;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.storage.MemoryDataStore2;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NullProgressListener;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

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
            outDataStore = new MemoryDataStore2();
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
        return saveAsOp.saveAsGeoTiff(inputCoverage, geoTifFile);
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
        com.sun.media.jai.util.ImageUtil.fillBackground(raster, raster.getBounds(),
                backgroundValues);
        writableImage.setData(raster);
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
            final String className = getClass().getSimpleName();
            if (className.equalsIgnoreCase(FeaturesToRasterOperation.class.getSimpleName())
                    || className.equalsIgnoreCase(GeometryToRasterOp.class.getSimpleName())) {
                // skip ====>
            } else {
                // Density, IDW ...expand by 0.5
                Extent.expandBy(CellSize / 2.0);

                // for kernel density + ArcGIS
                // Extent.expandBy(CellSize * 2.0);
            }
        }
    }

    protected DiskMemImage createDiskMemImage(GridCoverage2D srcCoverage,
            RasterPixelType transferType) {
        Extent = new ReferencedEnvelope(srcCoverage.getEnvelope());
        NoData = RasterHelper.getNoDataValue(srcCoverage);
        CellSize = RasterHelper.getCellSize(srcCoverage);

        // 원본 이미지와 타일의 갯수를 같게 생성한다.
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

    protected GridCoverage2D createGridCoverage(CharSequence name, PlanarImage tiledImage) {
        return createGridCoverage(name, tiledImage, 0, NoData, MinValue, MaxValue, Extent);
    }

    protected GridCoverage2D createGridCoverage(CharSequence name, PlanarImage tiledImage,
            int bandCount, double noDataValue, double minValue, double maxValue,
            ReferencedEnvelope coverageExtent) {

        if (tiledImage == null || coverageExtent == null) {
            LOGGER.log(Level.WARNING, "WritableRaster is null!");
            return null;
        }

        if (noDataValue == minValue) {
            noDataValue = minValue - 1;
        } else if (noDataValue == maxValue) {
            noDataValue = maxValue + 1;
        } else if (noDataValue > minValue && noDataValue < maxValue) {
            noDataValue = minValue - 1;
        }

        Color[] colors = new Color[] { Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED };

        CharSequence noDataName = Vocabulary.formatInternational(VocabularyKeys.NODATA);
        Category nan = new Category(noDataName, new Color[] { new Color(0, 0, 0, 0) },
                NumberRange.create(noDataValue, noDataValue), NumberRange.create(noDataValue,
                        noDataValue));

        Category values = new Category("values", colors, NumberRange.create(minValue, maxValue),
                NumberRange.create(minValue, maxValue));

        GridSampleDimension[] bands = null;
        GridSampleDimension band = null;

        band = new GridSampleDimension("Dimension", new Category[] { nan, values }, null);
        bands = new GridSampleDimension[] { band.geophysics(true) };

        // setting metadata
        final Map<CharSequence, Double> properties = new HashMap<CharSequence, Double>();
        properties.put("Maximum", Double.valueOf(maxValue));
        properties.put("Minimum", Double.valueOf(minValue));
        // properties.put("Mean", 1.0);
        // properties.put("StdDev", 1.0);
        properties.put(noDataName, Double.valueOf(noDataValue));
        properties.put("GC_NODATA", Double.valueOf(noDataValue));

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        return factory.create(name, tiledImage, coverageExtent, bands, null, properties);
    }
}
