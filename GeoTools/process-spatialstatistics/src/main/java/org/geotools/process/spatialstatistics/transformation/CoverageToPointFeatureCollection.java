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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.GeoTools;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.GridTransformer;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * CoverageToPoint SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CoverageToPointFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(CoverageToPointFeatureCollection.class);

    static String VALUE_FIELD = "Value";

    private SimpleFeatureType schema;

    private GridCoverage2D coverage;

    private int bandIndex = 0; // default

    public CoverageToPointFeatureCollection(GridCoverage2D coverage) {
        this(coverage, 0);
    }

    public CoverageToPointFeatureCollection(GridCoverage2D coverage, int bandIndex) {
        super(null);

        this.coverage = coverage;
        this.bandIndex = bandIndex;
        this.schema = createTemplateFeature(coverage);
    }

    private SimpleFeatureType createTemplateFeature(GridCoverage2D coverage) {
        String typeName = coverage.getName().toString();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(typeName, Point.class,
                coverage.getCoordinateReferenceSystem());
        schema = FeatureTypes.add(schema, typeName, Double.class);

        RasterPixelType pixelType = RasterHelper.getTransferType(coverage);
        switch (pixelType) {
        case BYTE:
        case SHORT:
        case INTEGER:
            schema = FeatureTypes.add(schema, VALUE_FIELD, Integer.class);
            break;
        case FLOAT:
        case DOUBLE:
            schema = FeatureTypes.add(schema, VALUE_FIELD, Double.class);
            break;
        default:
            schema = FeatureTypes.add(schema, VALUE_FIELD, Double.class);
            break;
        }

        return schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new CoverageToPointFeatureIterator(coverage, bandIndex, getSchema());
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

        private int row = 0;

        private int column = 0;

        private int bandIndex = 0; // default

        private double noData;

        private GridTransformer trans;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private int featureID = 0;

        private List<Coordinate> coordinates = new ArrayList<Coordinate>();

        public CoverageToPointFeatureIterator(GridCoverage2D coverage, int bandIndex,
                SimpleFeatureType schema) {
            this.bandIndex = bandIndex;
            this.noData = RasterHelper.getNoDataValue(coverage);
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = coverage.getName().toString();
            this.pixelType = RasterHelper.getTransferType(coverage);
            this.trans = new GridTransformer(coverage.getGridGeometry());

            PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
            this.readIter = RectIterFactory.create(inputImage, inputImage.getBounds());

            row = 0;
            this.readIter.startLines();
        }

        @Override
        public void close() {
            // nothing to do
        }

        private void extractValues() {
            coordinates.clear();
            column = 0;
            readIter.startPixels();
            while (!readIter.finishedPixels()) {
                double sampleValue = readIter.getSampleDouble(bandIndex);
                if (!SSUtils.compareDouble(noData, sampleValue)) {
                    Coordinate coord = trans.gridToWorldCoordinate(column, row);
                    coord.z = sampleValue;
                    coordinates.add(coord);
                }
                column++;
                readIter.nextPixel();
            }
            row++;
            readIter.nextLine();
        }

        public boolean hasNext() {
            while (next == null && !readIter.finishedLines()) {
                if (coordinates.size() == 0) {
                    extractValues();
                }

                if (coordinates.size() > 0) {
                    Coordinate coord = coordinates.get(0);

                    next = builder.buildFeature(buildID(typeName, ++featureID));
                    next.setDefaultGeometry(gf.createPoint(coord));
                    next.setAttribute(VALUE_FIELD, getPixelValue(coord.z, pixelType));

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