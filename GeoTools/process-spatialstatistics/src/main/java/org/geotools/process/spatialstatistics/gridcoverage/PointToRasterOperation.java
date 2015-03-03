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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Converts point features to a raster dataset. <br>
 * Any feature class (geodatabase, shapefile or coverage) containing point or multipoint features can be converted to a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointToRasterOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(PointToRasterOperation.class);

    // http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#/How_Point_To_Raster_works/001200000035000000/

    /**
     * The method to determine how the cell will be assigned a value when more than one feature falls within a cell.
     * 
     */
    public enum AssignmentType {

        /**
         * If there is more than one feature within the cell, the one with the most common attribute, in <field>, is assigned to the cell. If they
         * have the same number of common attributes, the one with the lowest FID is used.
         * 
         */
        MOST_FREQUENT,

        /**
         * The sum of the attributes of all the points within the cell (not valid for string data).
         * 
         */
        SUM,

        /**
         * The mean of the attributes of all the points within the cell (not valid for string data).
         * 
         */
        MEAN,

        /**
         * The standard deviation of attributes of all the points within the cell. If there are less than two points in the cell, the cellis assigned
         * NoData (not valid for string data).
         * 
         */
        STANDARD_DEVIATION,

        /**
         * The maximum value of the attributes of the points within the cell (not valid for string data).
         * 
         */
        MAXIMUM,

        /**
         * The minimum value of the attributes of the points within the cell (not valid for string data).
         * 
         */
        MINIMUM,

        /**
         * The range of the attributes of the points within the cell (not valid for string data).
         * 
         */
        RANGE,

        /**
         * The number of points within the cell.
         * 
         */
        COUNT
    }

    private enum ValueType {
        COUNT, CONSTANT, FIELD
    }

    DiskMemImage outputImage = null;

    AssignmentType assignType = AssignmentType.COUNT;

    ValueType valueType = ValueType.COUNT;

    public GridCoverage2D executePointCount(SimpleFeatureCollection pointFeatures) {
        return executePointCount(pointFeatures, Short.MIN_VALUE);
    }

    public GridCoverage2D executePointCount(SimpleFeatureCollection pointFeatures, int defaultValue) {
        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, Short.MIN_VALUE);

        outputImage = this.createDiskMemImage(Extent, RasterPixelType.SHORT);

        // initialize nodata value
        initializeDefaultValue(outputImage, (double) defaultValue);

        int intValue = 0;

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), Extent);

        GridTransformer trans = new GridTransformer(Extent, CellSize);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = pointFeatures.subCollection(filter).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // Multipoints are treated as a set of individual points.
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                for (int iPart = 0; iPart < multiPoint.getNumGeometries(); iPart++) {
                    final Coordinate realPos = multiPoint.getGeometryN(iPart).getCoordinate();
                    final GridCoordinates2D gridPos = trans.worldToGrid(realPos);
                    if (trans.contains(gridPos.x, gridPos.y)) {
                        final int sampleVal = outputImage.getSample(gridPos.x, gridPos.y, 0);
                        if (sampleVal == NoData || sampleVal == defaultValue) {
                            intValue = 1;
                        } else {
                            intValue = sampleVal + 1;
                        }

                        // set sample value
                        outputImage.setSample(gridPos.x, gridPos.y, 0, intValue);
                        updateStatistics(intValue);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        return createGridCoverage("PointToRaster", outputImage);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, int gridValue) {
        return execute(pointFeatures, Integer.toString(gridValue), AssignmentType.MOST_FREQUENT);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String fieldOrValue,
            AssignmentType assignmentType) {
        // get transfer type
        int gridValue = 0;
        if (StringHelper.isNullOrEmpty(fieldOrValue) || fieldOrValue.toUpperCase().contains("NONE")) {
            this.assignType = AssignmentType.COUNT;
            PixelType = RasterPixelType.SHORT;
            valueType = ValueType.COUNT;
        } else if (StringHelper.isNumeric(fieldOrValue)) {
            this.assignType = AssignmentType.MOST_FREQUENT;
            valueType = ValueType.CONSTANT;
            gridValue = Integer.valueOf(fieldOrValue);
            PixelType = gridValue < Short.MAX_VALUE ? RasterPixelType.SHORT
                    : RasterPixelType.INTEGER;
        } else {
            this.assignType = assignmentType;
            valueType = ValueType.FIELD;
            if (this.assignType != AssignmentType.COUNT) {
                return executeField(pointFeatures, fieldOrValue);
            }
        }

        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, RasterHelper.getDefaultNoDataValue(PixelType));

        // create image
        outputImage = this.createDiskMemImage(Extent, PixelType);

        // initialize nodata value
        initializeDefaultValue(outputImage, this.NoData);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), Extent);

        GridTransformer trans = new GridTransformer(Extent, CellSize);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = pointFeatures.subCollection(filter).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // Multipoints are treated as a set of individual points.
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                for (int iPart = 0; iPart < multiPoint.getNumGeometries(); iPart++) {
                    final Coordinate realPos = multiPoint.getGeometryN(iPart).getCoordinate();
                    final GridCoordinates2D gridPos = trans.worldToGrid(realPos);
                    if (trans.contains(gridPos.x, gridPos.y)) {
                        if (valueType == ValueType.COUNT) {
                            gridValue = outputImage.getSample(gridPos.x, gridPos.y, 0);
                            gridValue = gridValue == NoData ? 1 : gridValue++;
                        }

                        outputImage.setSample(gridPos.x, gridPos.y, 0, gridValue);
                        updateStatistics(gridValue);
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        return createGridCoverage("PointToRaster", outputImage);
    }

    private GridCoverage2D executeField(SimpleFeatureCollection pointFeatures, String field) {
        switch (assignType) {
        case MOST_FREQUENT:
        case SUM:
        case MINIMUM:
        case MAXIMUM:
        case RANGE:
            PixelType = RasterHelper.getTransferType(pointFeatures.getSchema(), field);
            break;
        case MEAN:
        case STANDARD_DEVIATION:
            PixelType = RasterPixelType.FLOAT;
            break;
        }

        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, RasterHelper.getDefaultNoDataValue(PixelType));

        // create image
        outputImage = this.createDiskMemImage(Extent, PixelType);

        // initialize nodata value
        initializeDefaultValue(outputImage, this.NoData);

        PointVisitor pointVisitor = new PointVisitor();

        field = FeatureTypes.validateProperty(pointFeatures.getSchema(), field);
        int fieldIndex = pointFeatures.getSchema().indexOf(field);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.bbox(ff.property(the_geom), Extent);

        GridTransformer trans = new GridTransformer(Extent, CellSize);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = pointFeatures.subCollection(filter).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                // Multipoints are treated as a set of individual points.
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                for (int iPart = 0; iPart < multiPoint.getNumGeometries(); iPart++) {
                    final Coordinate realPos = multiPoint.getGeometryN(iPart).getCoordinate();
                    final GridCoordinates2D gridPos = trans.worldToGrid(realPos);
                    if (trans.contains(gridPos.x, gridPos.y)) {
                        pointVisitor.visit(gridPos, feature.getAttribute(fieldIndex));
                    }
                }
            }
        } finally {
            featureIter.close();
        }

        Hashtable<GridCoordinates2D, List<Double>> results = pointVisitor.getResult();

        if (results.size() > 0) {
            Iterator<GridCoordinates2D> iterator = results.keySet().iterator();
            while (iterator.hasNext()) {
                GridCoordinates2D pos = iterator.next();
                List<Double> valueList = results.get(pos);

                StatisticsVisitorResult statics = pointVisitor.getStatistics(valueList);

                Number value = NoData;
                switch (assignType) {
                case MOST_FREQUENT:
                    if (valueList.size() <= 2) {
                        value = valueList.get(0);
                    } else {
                        Collections.sort(valueList);

                        // calculate frequency
                        // Priority field is only used with the MOST_FREQUENT option.

                        value = valueList.get(0);
                    }
                    break;
                case COUNT:
                    value = statics.getCount();
                    break;
                case SUM:
                    value = statics.getSum();
                    break;
                case MEAN:
                    value = statics.getMean();
                    break;
                case MINIMUM:
                    value = statics.getMinimum();
                    break;
                case MAXIMUM:
                    value = statics.getMaximum();
                    break;
                case STANDARD_DEVIATION:
                    value = statics.getStandardDeviation();
                    break;
                case RANGE:
                    value = statics.getRange();
                    break;
                }

                outputImage.setSample(pos.x, pos.y, 0, value.doubleValue());
                updateStatistics(value.doubleValue());
            }
        }

        return createGridCoverage("PointToRaster", outputImage, 0, NoData, MinValue, MaxValue,
                Extent);
    }

    final class PointVisitor {
        Hashtable<GridCoordinates2D, List<Double>> fSet = null;

        public PointVisitor() {
            fSet = new Hashtable<GridCoordinates2D, List<Double>>();
        }

        public Hashtable<GridCoordinates2D, List<Double>> getResult() {
            return fSet;
        }

        public void visit(GridCoordinates2D pos, Object value) {
            List<Double> valueList = fSet.get(pos);
            if (valueList == null) {
                valueList = new ArrayList<Double>();
            }

            valueList.add(Double.parseDouble(value.toString()));

            fSet.put(pos, valueList);
        }

        public StatisticsVisitorResult getStatistics(List<Double> valueList) {
            StatisticsVisitorResult statistics = new StatisticsVisitorResult();

            int count = valueList.size();
            double minVal = Double.MAX_VALUE;
            double maxVal = Double.MIN_VALUE;
            double sumVal = 0;

            double multiVal = 0;
            for (Double curVal : valueList) {
                sumVal += curVal;
                multiVal += curVal * curVal;
                maxVal = Math.max(maxVal, curVal);
                minVal = Math.min(minVal, curVal);
            }

            double variance = 0;
            if (count > 0) {
                double meanVal = sumVal / count;
                variance = (multiVal / count) - (meanVal * meanVal);
            }

            statistics.setCount(count);
            statistics.setMinimum(minVal);
            statistics.setMaximum(maxVal);
            statistics.setSum(sumVal);
            statistics.setVariance(variance);

            return statistics;
        }
    }
}
