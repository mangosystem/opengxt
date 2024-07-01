/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.util.List;
import java.util.logging.Logger;

import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Triangle;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

/**
 * Interpolates a raster surface from points using an Triangulated Irregular Network(TIN) technique.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterInterpolationTINOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterInterpolationTINOperation.class);

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    private FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    private double proximalTolerance = 0.0d;

    public RasterInterpolationTINOperation() {

    }

    public double getProximalTolerance() {
        return proximalTolerance;
    }

    public void setProximalTolerance(double proximalTolerance) {
        this.proximalTolerance = proximalTolerance;
    }

    public GridCoverage2D execute(SimpleFeatureCollection features, String inputField) {
        return execute(features, inputField, RasterPixelType.FLOAT);
    }

    public GridCoverage2D execute(SimpleFeatureCollection features, String inputField,
            RasterPixelType pixelType) {
        return execute(features, ff.property(inputField), pixelType);
    }

    public GridCoverage2D execute(SimpleFeatureCollection features, Expression inputField) {
        return execute(features, inputField, RasterPixelType.FLOAT);
    }

    public GridCoverage2D execute(SimpleFeatureCollection features, Expression inputField,
            RasterPixelType pixelType) {
        // create delaunay triangulation
        List<Coordinate> coordinates = getCoordinateList(features, inputField);

        // Gets the faces of the computed triangulation as a GeometryCollection of Polygon.
        DelaunayTriangulationBuilder vdBuilder = new DelaunayTriangulationBuilder();
        vdBuilder.setSites(coordinates);
        vdBuilder.setTolerance(proximalTolerance);
        Geometry triangles = vdBuilder.getTriangles(gf);
        coordinates.clear();

        // calculate extent & cellsize
        calculateExtentAndCellSize(features, RasterHelper.getDefaultNoDataValue(pixelType));

        // create image & write pixels
        final DiskMemImage oi = createDiskMemImage(gridExtent, pixelType);
        initializeDefaultValue(oi, this.noData);

        final GridTransformer trans = new GridTransformer(gridExtent, pixelSizeX, pixelSizeY);

        CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
        ReferencedEnvelope envelope = null;

        for (int index = 0; index < triangles.getNumGeometries(); index++) {
            Geometry triangle = triangles.getGeometryN(index);
            if (triangle == null || triangle.isEmpty()) {
                continue;
            }

            PreparedGeometry preparedGeom = PreparedGeometryFactory.prepare(triangle);

            Coordinate[] coords = triangle.getCoordinates();

            Coordinate v0 = coords[0];
            Coordinate v1 = coords[1];
            Coordinate v2 = coords[2];

            // write raster
            envelope = new ReferencedEnvelope(triangle.getEnvelopeInternal(), crs);
            java.awt.Rectangle rect = trans.worldToGridBounds(envelope);

            WritableRectIter writer = RectIterFactory.createWritable(oi, rect);

            writer.startLines();
            int y = rect.y;
            while (!writer.finishedLines()) {
                writer.startPixels();
                int x = rect.x;
                while (!writer.finishedPixels()) {
                    Coordinate p = trans.gridToWorldCoordinate(x, y);

                    if (!preparedGeom.disjoint(gf.createPoint(p))) {
                        double interpolated = Triangle.interpolateZ(p, v0, v1, v2);
                        if (Double.isNaN(interpolated) || Double.isInfinite(interpolated)) {
                            interpolated = this.noData;
                        }

                        writer.setSample(0, interpolated);
                        updateStatistics(interpolated);
                    }

                    writer.nextPixel();
                    x++;
                }
                writer.nextLine();
                y++;
            }
        }

        return createGridCoverage("TIN", oi);
    }

    private List<Coordinate> getCoordinateList(SimpleFeatureCollection features,
            Expression inputField) {
        CoordinateList coordinateList = new CoordinateList();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                Double zValue = inputField.evaluate(feature, Double.class);
                if (zValue == null || zValue.isNaN() || zValue.isInfinite()) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate[] coords = geometry.getCoordinates();
                int size = geometry instanceof MultiPolygon || geometry instanceof Polygon
                        ? coords.length - 1
                        : coords.length;

                for (int i = 0; i < size; i++) {
                    Coordinate xyzCoordnate = new Coordinate(coords[i]);
                    xyzCoordnate.setZ(zValue);
                    coordinateList.add(xyzCoordnate, false);
                }
            }
        } finally {
            featureIter.close();
        }

        return coordinateList;
    }
}
