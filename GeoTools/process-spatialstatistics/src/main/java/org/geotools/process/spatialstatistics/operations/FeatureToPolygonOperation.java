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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

/**
 * Creates a feature class containing polygons generated from areas enclosed by input line or polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class FeatureToPolygonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(FeatureToPolygonOperation.class);

    private double tolerance = 0.001d;

    private SimpleFeatureCollection labelFeatures = null;

    private String geomField = null;

    public FeatureToPolygonOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, double tolerance,
            SimpleFeatureCollection labelFeatures) throws IOException {
        this.tolerance = tolerance;

        boolean isPolygon = FeatureTypes.getSimpleShapeType(inputFeatures) == SimpleShapeType.POLYGON;

        if (labelFeatures != null) {
            labelFeatures = DataUtils.toSpatialIndexFeatureCollection(labelFeatures);
            geomField = labelFeatures.getSchema().getGeometryDescriptor().getLocalName();
        }

        if (isPolygon) {
            return executePolygon(inputFeatures, labelFeatures);
        } else {
            return executeLine(inputFeatures, labelFeatures);
        }
    }

    private SimpleFeatureCollection executeLine(SimpleFeatureCollection lineFeatures,
            SimpleFeatureCollection labelFeatures) throws IOException {
        SimpleFeatureType featureType = createSchema(lineFeatures, labelFeatures);
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        List<Polygon> polygons = polygonize(lineFeatures);
        try {
            for (Geometry polygon : polygons) {
                SimpleFeature newFeature = featureWriter.buildFeature();

                SimpleFeature labelFeature = findLabelFeature(polygon);
                if (labelFeature != null) {
                    featureWriter.copyAttributes(labelFeature, newFeature, false);
                }
                newFeature.setDefaultGeometry(polygon);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private SimpleFeatureCollection executePolygon(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection labelFeatures) throws IOException {
        FeatureToLineOperation operation = new FeatureToLineOperation();
        return execute(operation.execute(inputFeatures, false), tolerance, labelFeatures);
    }

    private SimpleFeature findLabelFeature(Geometry polygon) {
        if (labelFeatures == null) {
            return null;
        }

        Filter filter = getIntersectsFilter(geomField, polygon);
        SimpleFeatureIterator featureIter = labelFeatures.subCollection(filter).features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                return feature;
            }
        } finally {
            featureIter.close();
        }

        return null;
    }

    private SimpleFeatureType createSchema(SimpleFeatureCollection lineFeatures,
            SimpleFeatureCollection labelFeatures) {
        SimpleFeatureType schema = lineFeatures.getSchema();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        String name = schema.getTypeName();
        Class<?> geomBinding = Polygon.class;

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(name, geomBinding, crs);
        if (labelFeatures != null) {
            featureType = FeatureTypes.build(labelFeatures.getSchema(), name, geomBinding, crs);
        }

        return featureType;
    }

    @SuppressWarnings("unchecked")
    private List<Polygon> polygonize(SimpleFeatureCollection lineFeatures) {
        List<LineString> list = new ArrayList<LineString>();
        SimpleFeatureIterator featureIter = lineFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry multi = (Geometry) feature.getDefaultGeometry();
                for (int index = 0; index < multi.getNumGeometries(); index++) {
                    LineString part = (LineString) multi.getGeometryN(index);
                    // part = exendLine(part, tolerance, tolerance);
                    list.add(part);
                }
            }
        } finally {
            featureIter.close();
        }

        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(gf.buildGeometry(list).union());

        return (List<Polygon>) polygonizer.getPolygons();
    }

    @SuppressWarnings("unused")
    private LineString exendLine(LineString segment, double fromOffset, double toOffset) {
        Coordinate[] coordinates = ((LineString) segment.clone()).getCoordinates();

        LineSegment line = new LineSegment();
        if (fromOffset > 0) {
            line.p0 = coordinates[0];
            line.p1 = coordinates[1];
            coordinates[0] = offset(line.p0, line.angle(), -fromOffset);
        }

        if (toOffset > 0) {
            line.p0 = coordinates[coordinates.length - 2];
            line.p1 = coordinates[coordinates.length - 1];
            coordinates[coordinates.length - 1] = offset(line.p1, line.angle(), toOffset);
        }

        return segment.getFactory().createLineString(coordinates);
    }

    private Coordinate offset(Coordinate coordinate, double angle, double distance) {
        double newX = coordinate.x + distance * Math.cos(angle);
        double newY = coordinate.y + distance * Math.sin(angle);
        return new Coordinate(newX, newY);
    }
}