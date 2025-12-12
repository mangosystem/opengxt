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
package org.geotools.process.spatialstatistics.transformation;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.media.iterators.RectIterFactory;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.GridTransformer;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * CoverageToGrid SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CoverageToGridFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(CoverageToGridFeatureCollection.class);

    static final String ID_FIELD = "id";

    static final String VALUE_FIELD = "value";

    private SimpleFeatureType schema;

    private GridCoverage2D coverage;

    private int bandIndex = 0;

    private String valueField = VALUE_FIELD;

    private boolean retainNoData = Boolean.FALSE;

    public CoverageToGridFeatureCollection(GridCoverage2D coverage) {
        this(coverage, 0);
    }

    public CoverageToGridFeatureCollection(GridCoverage2D coverage, Integer bandIndex) {
        this(coverage, bandIndex, VALUE_FIELD);
    }

    public CoverageToGridFeatureCollection(GridCoverage2D coverage, Integer bandIndex,
            String valueField) {
        this(coverage, bandIndex, valueField, Boolean.FALSE);
    }

    public CoverageToGridFeatureCollection(GridCoverage2D coverage, Integer bandIndex,
            String valueField, boolean retainNoData) {
        super(null);

        this.coverage = coverage;
        this.bandIndex = bandIndex;
        this.valueField = valueField == null || valueField.isEmpty() ? VALUE_FIELD : valueField;
        this.retainNoData = retainNoData;
        this.schema = createTemplateFeature(coverage);
    }

    private SimpleFeatureType createTemplateFeature(GridCoverage2D coverage) {
        String typeName = coverage.getName().toString();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(typeName, Polygon.class,
                coverage.getCoordinateReferenceSystem());
        schema = FeatureTypes.add(schema, ID_FIELD, Integer.class);

        RasterPixelType pixelType = RasterHelper.getTransferType(coverage);
        switch (pixelType) {
        case BYTE:
        case SHORT:
        case INTEGER:
            schema = FeatureTypes.add(schema, typeName, Integer.class);
            schema = FeatureTypes.add(schema, valueField, Integer.class);
            break;
        case FLOAT:
        case DOUBLE:
            schema = FeatureTypes.add(schema, typeName, Double.class);
            schema = FeatureTypes.add(schema, valueField, Double.class);
            break;
        default:
            schema = FeatureTypes.add(schema, typeName, Double.class);
            schema = FeatureTypes.add(schema, valueField, Double.class);
            break;
        }

        return schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new CoverageToPointFeatureIterator(coverage, bandIndex, valueField, retainNoData,
                getSchema());
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public int size() {
        return DataUtilities.count(features());
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return new ReferencedEnvelope(coverage.getEnvelope());
    }

    static class CoverageToPointFeatureIterator implements SimpleFeatureIterator {
        private GeometryFactory gf = JTSFactoryFinder
                .getGeometryFactory(GeoTools.getDefaultHints());

        private RasterPixelType pixelType;

        private RectIter readIter;

        private String typeName;

        private int currentRow = 0;

        private int rowCount = 0;

        private int bandIndex = 0;

        private String valueField = "value";

        private boolean retainNoData = Boolean.FALSE;

        private double noData;

        private GridTransformer trans;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private int featureID = 0;

        private List<Coordinate> coordinates = new ArrayList<Coordinate>();

        private double width = 0;

        private double height = 0;

        public CoverageToPointFeatureIterator(GridCoverage2D coverage, int bandIndex,
                String valueField, boolean retainNoData, SimpleFeatureType schema) {
            this.bandIndex = bandIndex;
            this.valueField = valueField;
            this.retainNoData = retainNoData;
            this.noData = RasterHelper.getNoDataValue(coverage);
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = coverage.getName().toString();
            this.pixelType = RasterHelper.getTransferType(coverage);

            GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
            ReferencedEnvelope extent = new ReferencedEnvelope(gridGeometry2D.getEnvelope());
            AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

            double dx = Math.abs(gridToWorld.getScaleX());
            double dy = Math.abs(gridToWorld.getScaleY());
            this.trans = new GridTransformer(extent, dx, dy);
            // this.trans = new GridTransformer(coverage);

            width = dx / 2.0;
            height = dy / 2.0;

            PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
            this.readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

            currentRow = 0;
            rowCount = inputImage.getHeight();
            this.readIter.startLines();
        }

        @Override
        public void close() {
            // nothing to do
        }

        private void extractValues() {
            coordinates.clear();
            int column = 0; // bounds.x;
            int row = currentRow; // bounds.y + currentRow;
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                double sampleValue = readIter.getSampleDouble(bandIndex);
                if (retainNoData) {
                    Coordinate coord = trans.gridToWorldCoordinate(column, row);
                    coord.z = sampleValue;
                    coordinates.add(coord);
                } else {
                    if (!SSUtils.compareDouble(noData, sampleValue)) {
                        Coordinate coord = trans.gridToWorldCoordinate(column, row);
                        coord.z = sampleValue;
                        coordinates.add(coord);
                    }
                }
                column++;
                readIter.nextPixel();
            }
            currentRow++;
            readIter.nextLine();
        }

        public boolean hasNext() {
            while (next == null && (rowCount > currentRow || coordinates.size() > 0)) {
                if (coordinates.size() == 0) {
                    extractValues();
                }

                if (coordinates.size() > 0) {
                    Coordinate coord = coordinates.get(0);
                    Geometry rectangle = createPolygon(coord, width, height);
                    Object value = getPixelValue(coord.z, pixelType);

                    next = builder.buildFeature(buildID(typeName, ++featureID));
                    next.setDefaultGeometry(rectangle);
                    next.setAttribute(ID_FIELD, featureID);
                    next.setAttribute(typeName, value);
                    next.setAttribute(valueField, value);

                    coordinates.remove(0);
                }
            }

            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature result = next;
            next = null;
            return result;
        }

        private Geometry createPolygon(Coordinate coord, double width, double height) {
            Coordinate[] coords = new Coordinate[] {
                    new Coordinate(coord.x - width, coord.y - height),
                    new Coordinate(coord.x + width, coord.y - height),
                    new Coordinate(coord.x + width, coord.y + height),
                    new Coordinate(coord.x - width, coord.y + height),
                    new Coordinate(coord.x - width, coord.y - height) };

            LinearRing ring = gf.createLinearRing(coords);
            Geometry polygon = gf.createPolygon(ring, null);
            return polygon;
        }

        private Object getPixelValue(double curVal, RasterPixelType pixelType) {
            switch (pixelType) {
            case BYTE:
            case SHORT:
            case INTEGER:
                return (int) curVal;
            case FLOAT:
            case DOUBLE:
                return curVal;
            }

            return curVal;
        }
    }
}