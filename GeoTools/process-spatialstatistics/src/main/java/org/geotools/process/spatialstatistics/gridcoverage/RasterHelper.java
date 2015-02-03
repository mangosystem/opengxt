package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;

/**
 * Help class for GridCoverage2D
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHelper {
    protected static final Logger LOGGER = Logging.getLogger(RasterHelper.class);

    public static void describe(GridCoverage2D gc) {
        System.out.println("Name = " + gc.getName());
        System.out.println("CellSize = " + RasterHelper.getCellSize(gc));

        final RenderedImage image = gc.getRenderedImage();
        System.out.println("Colomns = " + image.getWidth());
        System.out.println("Rows = " + image.getHeight());
        System.out.println("NumberOfBands = " + gc.getNumSampleDimensions());

        System.out.println("PixelType = " + RasterHelper.getTransferType(gc));
        System.out.println("NoData = " + RasterHelper.getNoDataValue(gc));

        System.out.println("Extent = " + gc.getEnvelope());
    }

    public static GridCoverage2D openGridCoverage(File gridFile) {
        GridCoverage2D gc2D = null;

        // Reading the coverage through a file
        AbstractGridFormat format = GridFormatFinder.findFormat(gridFile);

        try {
            AbstractGridCoverage2DReader reader = format.getReader(gridFile);
            gc2D = reader.read(null);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gc2D;
    }

    public static GridCoverage2D openGeoTiffFile(String filePath) {
        GridCoverage2D gc2D = null;

        GeoTiffReader reader = null;
        try {
            File gridFile = new File(filePath);
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            reader = new GeoTiffReader(gridFile, hints);
            try {
                gc2D = reader.read(null);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } catch (DataSourceException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }

        return gc2D;
    }

    public static DirectPosition worldToGridPos(GridCoverage2D srcCoverage, DirectPosition realPos)
            throws TransformException {
        GridGeometry gg2D = srcCoverage.getGridGeometry();
        MathTransform gridToCRS = gg2D.getGridToCRS();
        MathTransform crsToGrid = gridToCRS.inverse();

        DirectPosition gridPos = new DirectPosition2D();
        crsToGrid.transform(realPos, gridPos);

        return gridPos;
    }

    public static Rectangle worldToGrid(GridCoverage2D srcCoverage, ReferencedEnvelope envelope)
            throws TransformException {
        GridGeometry gg2D = srcCoverage.getGridGeometry();

        MathTransform gridToCRS = gg2D.getGridToCRS();
        MathTransform crsToGrid = gridToCRS.inverse();

        DirectPosition lcGrid = new DirectPosition2D();
        crsToGrid.transform(envelope.getLowerCorner(), lcGrid);

        DirectPosition ucGrid = new DirectPosition2D();
        crsToGrid.transform(envelope.getUpperCorner(), ucGrid);

        int x = (int) Math.min(lcGrid.getOrdinate(0), ucGrid.getOrdinate(0));
        int y = (int) Math.min(lcGrid.getOrdinate(1), ucGrid.getOrdinate(1));

        int width = (int) Math.abs(lcGrid.getOrdinate(0) - ucGrid.getOrdinate(0));
        int height = (int) Math.abs(lcGrid.getOrdinate(1) - ucGrid.getOrdinate(1));

        return new Rectangle(x, y, width, height);
    }

    public static Dimension getDimension(ReferencedEnvelope inputBounds, double cellSize) {
        double aoiWidth = inputBounds.getMaxX() - inputBounds.getMinX();
        double aoiHeight = inputBounds.getMaxY() - inputBounds.getMinY();

        int columnCount = (int) Math.floor((aoiWidth / cellSize) + 0.5d);
        int rowCount = (int) Math.floor((aoiHeight / cellSize) + 0.5d);

        return new Dimension(columnCount, rowCount);
    }

    public static ReferencedEnvelope getResolvedEnvelope(ReferencedEnvelope inputBounds,
            double cellSize) {
        CoordinateReferenceSystem crs = inputBounds.getCoordinateReferenceSystem();

        Dimension gridDim = RasterHelper.getDimension(inputBounds, cellSize);

        // Recalculate coverage extent
        double maxX = inputBounds.getMinX() + (gridDim.width * cellSize);
        double maxY = inputBounds.getMinY() + (gridDim.height * cellSize);

        return new ReferencedEnvelope(inputBounds.getMinX(), maxX, inputBounds.getMinY(), maxY, crs);
    }

    public static double getCellSize(GridCoverage2D srcCoverage) {
        GridGeometry2D gridGeometry2D = srcCoverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        final double cellSizeX = Math.abs(gridToWorld.getScaleX());
        final double cellSizeY = Math.abs(gridToWorld.getScaleY());

        return Math.max(cellSizeX, cellSizeY);
    }

    public static double getSuggestedNoDataValue(GridCoverage2D srcCoverage) {
        double noDataValue = 0.0f;

        SampleDimension gridDim = srcCoverage.getSampleDimension(0);
        SampleDimensionType sdType = gridDim.getSampleDimensionType();

        /*
         * unsigned 1 bit = 0 to 1 unsigned 2 bit = 0 to 4 unsigned 4 bit = 0 to 16 unsigned 8 bit = 0 to 255 signed 8 bit = -128 to 127 unsigned 16
         * bit = 0 to 65535 signed 16 bit = -32768 to 32767 unsigned 32 bit = 0 to 4294967295 signed 32 bit = -2147483648 to 2147483647 floating point
         * 32 bit = -3.402823466e+38 to 3.402823466e+38
         */

        if (sdType == SampleDimensionType.UNSIGNED_1BIT) {
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.UNSIGNED_2BITS) {
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.UNSIGNED_4BITS) {
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.UNSIGNED_8BITS) {
            // ubyte
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.SIGNED_8BITS) {
            // byte
            noDataValue = Byte.MIN_VALUE; // -128
        } else if (sdType == SampleDimensionType.UNSIGNED_16BITS) {
            // ushort
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.SIGNED_16BITS) {
            // short
            noDataValue = Short.MIN_VALUE; // -32768
        } else if (sdType == SampleDimensionType.UNSIGNED_32BITS) {
            // uint
            noDataValue = 0;
        } else if (sdType == SampleDimensionType.SIGNED_32BITS) {
            // int
            noDataValue = Integer.MIN_VALUE; // -2147483648
        } else if (sdType == SampleDimensionType.REAL_32BITS) {
            // float
            noDataValue = -Float.MAX_VALUE;
        } else if (sdType == SampleDimensionType.REAL_64BITS) {
            // double
            noDataValue = -Double.MAX_VALUE;
        }

        return noDataValue;
    }

    public static double getNoDataValue(GridCoverage2D srcCoverage) {
        double noDataValue = 0.0f;

        // No data or GC_NODATA
        InternationalString noDataName = Vocabulary.formatInternational(VocabularyKeys.NODATA);
        Object objNoData = srcCoverage.getProperty(noDataName.toString());
        if (objNoData != null && objNoData instanceof Number) {
            return (Double) objNoData;
        } else {
            objNoData = srcCoverage.getProperty("GC_NODATA");
            if (objNoData != null && objNoData instanceof Number) {
                return (Double) objNoData;
            } else {
                SampleDimension gridDim = srcCoverage.getSampleDimension(0);
                double[] ndVals = gridDim.getNoDataValues();
                if (ndVals != null) {
                    noDataValue = ndVals[0];
                } else {
                    Double minVal = gridDim.getMinimumValue();
                    if (minVal == null || Double.isInfinite(minVal) || Double.isNaN(minVal)) {
                        noDataValue = getSuggestedNoDataValue(srcCoverage);
                    } else {
                        noDataValue = minVal;
                    }
                }
            }
        }
        return noDataValue;
    }

    public static double getDefaultNoDataValue(RasterPixelType transType) {
        switch (transType) {
        case BYTE:
            return Byte.MIN_VALUE;
        case SHORT:
            return Short.MIN_VALUE;
        case INTEGER:
            return Integer.MIN_VALUE;
        case FLOAT:
            return -Float.MAX_VALUE;
        case DOUBLE:
            return -Double.MAX_VALUE;
        }

        return -Float.MAX_VALUE;
    }

    public static RasterPixelType getTransferType(SimpleFeatureType srcType, String propertyName) {
        RasterPixelType transType = RasterPixelType.INTEGER;

        AttributeDescriptor atDesc = srcType.getDescriptor(propertyName);
        Class<?> binding = atDesc.getType().getBinding();

        if (binding.isAssignableFrom(Double.class)) {
            transType = RasterPixelType.FLOAT;
        } else if (binding.isAssignableFrom(Float.class)) {
            transType = RasterPixelType.FLOAT;
        } else if (binding.isAssignableFrom(BigDecimal.class)) {
            transType = RasterPixelType.INTEGER;
        } else if (binding.isAssignableFrom(Long.class)) {
            transType = RasterPixelType.INTEGER;
        } else if (binding.isAssignableFrom(Integer.class)) {
            transType = RasterPixelType.INTEGER;
        } else if (binding.isAssignableFrom(Short.class)) {
            transType = RasterPixelType.SHORT;
        } else if (binding.isAssignableFrom(Byte.class)) {
            transType = RasterPixelType.SHORT;
        }

        return transType;
    }

    public static RasterPixelType getTransferType(GridCoverage2D srcCoverage) {
        RasterPixelType transType = RasterPixelType.SHORT;

        SampleDimension gridDim = srcCoverage.getSampleDimension(0);
        SampleDimensionType sdType = gridDim.getSampleDimensionType();

        if (sdType == SampleDimensionType.UNSIGNED_1BIT) {
            transType = RasterPixelType.BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_2BITS) {
            transType = RasterPixelType.BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_4BITS) {
            transType = RasterPixelType.BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_8BITS) {
            transType = RasterPixelType.SHORT;
        } else if (sdType == SampleDimensionType.SIGNED_8BITS) {
            transType = RasterPixelType.SHORT;
        } else if (sdType == SampleDimensionType.UNSIGNED_16BITS) {
            transType = RasterPixelType.INTEGER;
        } else if (sdType == SampleDimensionType.SIGNED_16BITS) {
            transType = RasterPixelType.SHORT;
        } else if (sdType == SampleDimensionType.UNSIGNED_32BITS) {
            transType = RasterPixelType.FLOAT;
        } else if (sdType == SampleDimensionType.SIGNED_32BITS) {
            transType = RasterPixelType.INTEGER;
        } else if (sdType == SampleDimensionType.REAL_32BITS) {
            transType = RasterPixelType.FLOAT;
        } else if (sdType == SampleDimensionType.REAL_64BITS) {
            transType = RasterPixelType.DOUBLE;
        }

        return transType;
    }

    public static int getDataType(GridCoverage2D srcCoverage) {
        SampleDimension gridDim = srcCoverage.getSampleDimension(0);
        SampleDimensionType sdType = gridDim.getSampleDimensionType();

        int dataType = DataBuffer.TYPE_USHORT;

        if (sdType == SampleDimensionType.UNSIGNED_1BIT) {
            dataType = DataBuffer.TYPE_BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_2BITS) {
            dataType = DataBuffer.TYPE_BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_4BITS) {
            dataType = DataBuffer.TYPE_BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_8BITS) {
            // ubyte ++
            dataType = DataBuffer.TYPE_SHORT;
        } else if (sdType == SampleDimensionType.SIGNED_8BITS) {
            // byte
            dataType = DataBuffer.TYPE_BYTE;
        } else if (sdType == SampleDimensionType.UNSIGNED_16BITS) {
            // ushort ++
            dataType = DataBuffer.TYPE_INT;
        } else if (sdType == SampleDimensionType.SIGNED_16BITS) {
            // short
            dataType = DataBuffer.TYPE_SHORT;
        } else if (sdType == SampleDimensionType.UNSIGNED_32BITS) {
            // uint ++
            dataType = DataBuffer.TYPE_FLOAT;
        } else if (sdType == SampleDimensionType.SIGNED_32BITS) {
            // int
            dataType = DataBuffer.TYPE_INT;
        } else if (sdType == SampleDimensionType.REAL_32BITS) {
            // float
            dataType = DataBuffer.TYPE_FLOAT;
        } else if (sdType == SampleDimensionType.REAL_64BITS) {
            // double
            dataType = DataBuffer.TYPE_DOUBLE;
        }

        return dataType;
    }

    public static GridCoverage2D createGridCoverage(PlanarImage tiledImage, int bandCount,
            double noDataValue, double minValue, double maxValue, ReferencedEnvelope coverageExtent) {

        if (tiledImage == null || coverageExtent == null) {
            LOGGER.log(Level.FINE, "WritableRaster is null!");
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

        GridSampleDimension[] bandCol = null;
        GridSampleDimension band = null;

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        band = new GridSampleDimension("Dimension", new Category[] { nan, values }, null);
        bandCol = new GridSampleDimension[] { band.geophysics(true) };

        // setting metadata
        final Map<CharSequence, Double> properties = new HashMap<CharSequence, Double>();
        properties.put("Maximum", maxValue);
        properties.put("Minimum", minValue);
        // properties.put("Mean", 1.0);
        // properties.put("StdDev", 1.0);
        properties.put(noDataName, Double.valueOf(noDataValue));
        properties.put("GC_NODATA", Double.valueOf(noDataValue));

        return factory.create("GridCoverage2D", tiledImage, coverageExtent, bandCol, null,
                properties);
    }
}
