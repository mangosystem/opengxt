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

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.api.coverage.SampleDimension;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.util.logging.Logging;

/**
 * Describes the metadata of gridcoverage.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterDescribeOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterDescribeOperation.class);

    public RasterDescribeResult execute(GridCoverage2D coverage, Boolean detailed) {
        RasterDescribeResult desc = new RasterDescribeResult();

        desc.setName(coverage.getName().toString());

        RenderedImage image = coverage.getRenderedImage();
        desc.setColumns(image.getWidth());
        desc.setRows(image.getHeight());

        int numberofBands = coverage.getNumSampleDimensions();
        desc.setNumberOfBands(numberofBands);

        GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        double cellSizeX = Math.abs(gridToWorld.getScaleX());
        double cellSizeY = Math.abs(gridToWorld.getScaleY());

        desc.setCellSizeX(cellSizeX);
        desc.setCellSizeY(cellSizeY);

        // sdType.name() = REAL_32BITS
        SampleDimension gridDim = coverage.getSampleDimension(0); // default
        SampleDimensionType sdType = gridDim.getSampleDimensionType();

        desc.setPixelType(sdType.name());

        int pos = sdType.name().indexOf("_");
        if (pos >= 0) {
            desc.setPixelDepth(sdType.name().substring(pos + 1));
        } else {
            desc.setPixelDepth(sdType.name());
        }

        // common nodata
        desc.setNoData(RasterHelper.getNoDataValue(coverage));

        desc.setExtent(new Extent(coverage.getEnvelope()));

        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        if (crs != null) {
            desc.setSpatialReference(crs.toWKT());
        }

        // statistics
        if (detailed) {
            desc.setBands(calculateStatistics(coverage));
        }

        return desc;
    }

    private List<BandStatistics> calculateStatistics(GridCoverage2D coverage) {
        List<BandStatistics> bands = new ArrayList<BandStatistics>();

        double noData = RasterHelper.getNoDataValue(coverage);
        int numberofBands = coverage.getNumSampleDimensions();

        StatisticsVisitor[] visitor = new StatisticsVisitor[numberofBands];
        double[] ndvs = new double[numberofBands];
        double[] min = new double[numberofBands];
        double[] max = new double[numberofBands];
        String[] descs = new String[numberofBands];

        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);
        Arrays.fill(ndvs, noData);

        // extract band name & nodata
        for (int index = 0; index < numberofBands; index++) {
            SampleDimension dimension = coverage.getSampleDimension(index);
            descs[index] = dimension.getDescription().toString();

            double[] ndValues = dimension.getNoDataValues();
            if (ndValues != null) {
                ndvs[index] = ndValues[0]; // first
            } else {
                Double minVal = dimension.getMinimumValue();
                if (minVal != null && !Double.isInfinite(minVal) && !Double.isNaN(minVal)) {
                    ndvs[index] = minVal;
                }
            }

            visitor[index] = new StatisticsVisitor(new DoubleStrategy());
            visitor[index].setNoData(ndvs[index]);
        }

        // calculate min, max
        PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
        RectIter readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

        readIter.startLines();
        while (!readIter.finishedLines()) {
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                for (int index = 0; index < numberofBands; index++) {
                    double sampleValue = readIter.getSampleDouble(index);
                    if (!SSUtils.compareDouble(ndvs[index], sampleValue)) {
                        visitor[index].visit(sampleValue);
                    }
                }
                readIter.nextPixel();
            }
            readIter.nextLine();
        }

        for (int index = 0; index < numberofBands; index++) {
            StatisticsVisitorResult ret = visitor[index].getResult();

            BandStatistics band = new BandStatistics(index, descs[index], ndvs[index]);
            band.setCount(ret.getCount());
            band.setMinimun(ret.getMinimum());
            band.setMaximin(ret.getMaximum());
            band.setMean(ret.getMean());
            band.setSum(ret.getSum());
            band.setVariance(ret.getVariance());
            band.setStandardDeviation(ret.getStandardDeviation());

            bands.add(band);
        }

        return bands;
    }

    public static class BandStatistics {

        Integer bandIndex = 0;

        String description = "";

        Integer count = 0;

        Double minimun = Double.MIN_VALUE;

        Double maximin = Double.MAX_VALUE;

        Double mean = Double.NaN;

        Double sum = Double.NaN;

        Double variance = Double.NaN;

        Double standardDeviation = Double.NaN;

        Double noData = Double.NaN;

        public BandStatistics(Integer bandIndex, String description, Double noData) {
            this.bandIndex = bandIndex;
            this.description = description;
            this.noData = noData;
        }

        public Integer getBandIndex() {
            return bandIndex;
        }

        public void setBandIndex(Integer bandIndex) {
            this.bandIndex = bandIndex;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getMinimun() {
            return minimun;
        }

        public void setMinimun(Double minimun) {
            this.minimun = minimun;
        }

        public Double getMaximin() {
            return maximin;
        }

        public void setMaximin(Double maximin) {
            this.maximin = maximin;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Double getMean() {
            return mean;
        }

        public void setMean(Double mean) {
            this.mean = mean;
        }

        public Double getSum() {
            return sum;
        }

        public void setSum(Double sum) {
            this.sum = sum;
        }

        public Double getVariance() {
            return variance;
        }

        public void setVariance(Double variance) {
            this.variance = variance;
        }

        public Double getStandardDeviation() {
            return standardDeviation;
        }

        public void setStandardDeviation(Double standardDeviation) {
            this.standardDeviation = standardDeviation;
        }

        public Double getNoData() {
            return noData;
        }

        public void setNoData(Double noData) {
            this.noData = noData;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("BandIndex: ").append(getBandIndex()).append(separator);
            sb.append("Description: ").append(getDescription()).append(separator);
            sb.append("Count: ").append(getCount()).append(separator);
            sb.append("Minimum: ").append(getMinimun()).append(separator);
            sb.append("Maximum: ").append(getMaximin()).append(separator);
            sb.append("Mean: ").append(getMean()).append(separator);
            sb.append("Sum: ").append(getSum()).append(separator);
            sb.append("Variance: ").append(getVariance()).append(separator);
            sb.append("StandardDeviation: ").append(getStandardDeviation()).append(separator);
            sb.append("NoData: ").append(getNoData()).append(separator);

            return sb.toString();
        }
    }

    public static class Extent {

        Double xMin = Double.valueOf(0);

        Double yMin = Double.valueOf(0);

        Double xMax = Double.valueOf(0);

        Double yMax = Double.valueOf(0);

        public Double getxMin() {
            return xMin;
        }

        public void setxMin(Double xMin) {
            this.xMin = xMin;
        }

        public Double getyMin() {
            return yMin;
        }

        public void setyMin(Double yMin) {
            this.yMin = yMin;
        }

        public Double getxMax() {
            return xMax;
        }

        public void setxMax(Double xMax) {
            this.xMax = xMax;
        }

        public Double getyMax() {
            return yMax;
        }

        public void setyMax(Double yMax) {
            this.yMax = yMax;
        }

        public Extent() {

        }

        public Extent(Bounds envelope) {
            this.xMin = envelope.getMinimum(0);
            this.yMin = envelope.getMinimum(1);
            this.xMax = envelope.getMaximum(0);
            this.yMax = envelope.getMaximum(1);
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("Extent").append(separator);
            sb.append("  XMin: ").append(getxMin()).append(separator);
            sb.append("  YMin: ").append(getyMin()).append(separator);
            sb.append("  XMax: ").append(getxMax()).append(separator);
            sb.append("  YMax: ").append(getyMax()).append(separator);

            return sb.toString();
        }
    }

    public static class RasterDescribeResult {

        String name;

        Integer columns = Integer.valueOf(0);

        Integer rows = Integer.valueOf(0);

        Integer numberOfBands = Integer.valueOf(0);

        Double cellSizeX = Double.valueOf(0);

        Double cellSizeY = Double.valueOf(0);

        String pixelType = "";

        String pixelDepth = "";

        Double noData = Double.NaN;

        Extent extent;

        String spatialReference = "";

        List<BandStatistics> bands = new ArrayList<BandStatistics>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getColumns() {
            return columns;
        }

        public void setColumns(Integer columns) {
            this.columns = columns;
        }

        public Integer getRows() {
            return rows;
        }

        public void setRows(Integer rows) {
            this.rows = rows;
        }

        public Integer getNumberOfBands() {
            return numberOfBands;
        }

        public void setNumberOfBands(Integer numberOfBands) {
            this.numberOfBands = numberOfBands;
        }

        public Double getCellSizeX() {
            return cellSizeX;
        }

        public void setCellSizeX(Double cellSizeX) {
            this.cellSizeX = cellSizeX;
        }

        public Double getCellSizeY() {
            return cellSizeY;
        }

        public void setCellSizeY(Double cellSizeY) {
            this.cellSizeY = cellSizeY;
        }

        public String getPixelType() {
            return pixelType;
        }

        public void setPixelType(String pixelType) {
            this.pixelType = pixelType;
        }

        public String getPixelDepth() {
            return pixelDepth;
        }

        public void setPixelDepth(String pixelDepth) {
            this.pixelDepth = pixelDepth;
        }

        public Double getNoData() {
            return noData;
        }

        public void setNoData(Double noData) {
            this.noData = noData;
        }

        public Extent getExtent() {
            return extent;
        }

        public void setExtent(Extent extent) {
            this.extent = extent;
        }

        public String getSpatialReference() {
            return spatialReference;
        }

        public void setSpatialReference(String spatialReference) {
            this.spatialReference = spatialReference;
        }

        public List<BandStatistics> getBands() {
            return bands;
        }

        public void setBands(List<BandStatistics> bands) {
            this.bands = bands;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            StringBuffer sb = new StringBuffer();
            sb.append("Name: ").append(name).append(separator);
            sb.append("Columns: ").append(columns).append(separator);
            sb.append("Rows: ").append(rows).append(separator);
            sb.append("NumberOfBands: ").append(numberOfBands).append(separator);
            sb.append("CellSizeX: ").append(cellSizeX).append(separator);
            sb.append("CellSizeY: ").append(cellSizeY).append(separator);
            sb.append("PixelType: ").append(pixelType).append(separator);
            sb.append("PixelDepth: ").append(pixelDepth).append(separator);
            sb.append("NoData: ").append(noData).append(separator).append(separator);

            sb.append(extent.toString()).append(separator);

            for (int k = 0; k < getBands().size(); k++) {
                BandStatistics band = getBands().get(k);
                sb.append(band.toString()).append(separator);
            }

            sb.append("SpatialReference: ").append(separator).append(spatialReference.toString());

            return sb.toString();
        }
    }
}