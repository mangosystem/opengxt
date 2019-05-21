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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.enumeration.PointAssignmentType;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Converts point features to a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsToRasterOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(PointsToRasterOperation.class);

    private enum ValueType {
        COUNT, CONSTANT, FIELD
    }

    private DiskMemImage outputImage = null;

    private PointAssignmentType cellAssignment = PointAssignmentType.Count;

    private ValueType valueType = ValueType.COUNT;

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

        GridTransformer trans = new GridTransformer(Extent, CellSizeX, CellSizeY);
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

        return createGridCoverage("PointsToRaster", outputImage);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, int gridValue) {
        return execute(pointFeatures, Integer.toString(gridValue), PointAssignmentType.MostFrequent);
    }

    public GridCoverage2D execute(SimpleFeatureCollection pointFeatures, String fieldOrValue,
            PointAssignmentType cellAssignment) {
        // get transfer type
        int gridValue = 0;
        if (StringHelper.isNullOrEmpty(fieldOrValue) || fieldOrValue.toUpperCase().contains("NONE")) {
            this.cellAssignment = PointAssignmentType.Count;
            PixelType = RasterPixelType.SHORT;
            valueType = ValueType.COUNT;
        } else if (StringHelper.isNumeric(fieldOrValue)) {
            this.cellAssignment = PointAssignmentType.MostFrequent;
            valueType = ValueType.CONSTANT;
            gridValue = Integer.valueOf(fieldOrValue);
            PixelType = RasterPixelType.INTEGER;
        } else {
            this.cellAssignment = cellAssignment;
            valueType = ValueType.FIELD;
            return executeField(pointFeatures, fieldOrValue);
        }

        // calculate extent & cellsize
        calculateExtentAndCellSize(pointFeatures, RasterHelper.getDefaultNoDataValue(PixelType));

        // create image
        outputImage = this.createDiskMemImage(Extent, PixelType);

        // initialize nodata value
        initializeDefaultValue(outputImage, this.NoData);

        GridTransformer trans = new GridTransformer(Extent, CellSizeX, CellSizeY);
        SimpleFeatureIterator featureIter = pointFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                if (multiPoint == null || multiPoint.isEmpty()) {
                    continue;
                }

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

        return createGridCoverage("PointsToRaster", outputImage);
    }

    private GridCoverage2D executeField(SimpleFeatureCollection pointFeatures, String field) {
        switch (cellAssignment) {
        case MostFrequent:
        case Sum:
        case Minimum:
        case Maximum:
        case Range:
            PixelType = RasterHelper.getTransferType(pointFeatures.getSchema(), field);
            break;
        case Mean:
        case StandardDeviation:
            PixelType = RasterPixelType.FLOAT;
            break;
        default:
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

        GridTransformer trans = new GridTransformer(Extent, CellSizeX, CellSizeY);
        SimpleFeatureIterator featureIter = pointFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multiPoint = (Geometry) feature.getDefaultGeometry();
                if (multiPoint == null || multiPoint.isEmpty()) {
                    continue;
                }

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
                switch (cellAssignment) {
                case MostFrequent:
                    if (valueList.size() <= 2) {
                        value = valueList.get(0);
                    } else {
                        Collections.sort(valueList);

                        // TODO code here
                        value = valueList.get(0);
                    }
                    break;
                case Count:
                    value = statics.getCount();
                    break;
                case Sum:
                    value = statics.getSum();
                    break;
                case Mean:
                    value = statics.getMean();
                    break;
                case Minimum:
                    value = statics.getMinimum();
                    break;
                case Maximum:
                    value = statics.getMaximum();
                    break;
                case StandardDeviation:
                    value = statics.getStandardDeviation();
                    break;
                case Range:
                    value = statics.getRange();
                    break;
                }

                outputImage.setSample(pos.x, pos.y, 0, value.doubleValue());
                updateStatistics(value.doubleValue());
            }
        }

        return createGridCoverage("PointsToRaster", outputImage, 0, NoData, MinValue, MaxValue,
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