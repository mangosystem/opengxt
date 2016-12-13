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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;

/**
 * Creates a random point featurecollection.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class RandomPointsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(RandomPointsOperation.class);

    private com.vividsolutions.jts.shape.random.RandomPointsBuilder builder;

    private CoordinateReferenceSystem crs;

    public RandomPointsOperation() {

    }

    public SimpleFeatureCollection execute(ReferencedEnvelope extent, int pointCount)
            throws IOException {
        builder = new RandomPointsBuilder(gf);
        crs = extent.getCoordinateReferenceSystem();
        builder.setExtent(extent);

        return execute(pointCount);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection boundsSource, int pointCount)
            throws IOException {
        builder = new RandomPointsBuilder(gf);
        crs = boundsSource.getSchema().getCoordinateReferenceSystem();
        Geometry maskPolygon = unionFeatures(boundsSource);
        if (maskPolygon == null || maskPolygon.isEmpty()) {
            builder.setExtent(boundsSource.getBounds());
            LOGGER.log(Level.WARNING,
                    "Failed to create mask polygon, random points builder will use feature's boundary");
        } else {
            builder.setExtent(maskPolygon);
        }

        return execute(pointCount);
    }

    private SimpleFeatureCollection execute(int pointCount) throws IOException {
        builder.setNumPoints(pointCount);

        SimpleFeatureType schema = createSchema(false);
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            Geometry multiPoints = builder.getGeometry();
            for (int i = 0; i < multiPoints.getNumGeometries(); i++) {
                Point point = (Point) multiPoints.getGeometryN(i);

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setAttribute("weight", 1);
                newFeature.setDefaultGeometry(point);

                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    public SimpleFeatureCollection executeperFeatures(SimpleFeatureCollection polygonFeatures,
            int pointCount) throws IOException {
        try {
            Expression expression = ECQL.toExpression(String.valueOf(pointCount));
            return executeperFeatures(polygonFeatures, expression);
        } catch (CQLException e1) {
            LOGGER.log(Level.FINER, e1.getMessage(), e1);
        }
        return null;
    }

    public SimpleFeatureCollection executeperFeatures(SimpleFeatureCollection polygonFeatures,
            Expression expression) throws IOException {
        builder = new RandomPointsBuilder(gf);
        crs = polygonFeatures.getSchema().getCoordinateReferenceSystem();

        SimpleFeatureType schema = createSchema(true);
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        SimpleFeatureIterator featureIter = polygonFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Object value = expression.evaluate(feature);
                if (value == null) {
                    continue;
                }

                Integer pointCount = Converters.convert(value, Integer.class);
                if (pointCount == null || pointCount == 0) {
                    continue;
                }

                builder.setExtent(geometry);
                builder.setNumPoints(pointCount);

                Geometry multiPoints = builder.getGeometry();
                for (int i = 0; i < multiPoints.getNumGeometries(); i++) {
                    Point point = (Point) multiPoints.getGeometryN(i);

                    // create feature and set geometry
                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setAttribute("id", feature.getID());
                    newFeature.setAttribute("weight", 1);
                    newFeature.setDefaultGeometry(point);

                    featureWriter.write(newFeature);
                }
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private SimpleFeatureType createSchema(boolean createID) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("RandomPoints");
        typeBuilder.add("geom", Point.class, crs);
        if (createID) {
            typeBuilder.add("id", String.class);
        }
        typeBuilder.add("weight", Integer.class);
        return typeBuilder.buildFeatureType();
    }

    private Geometry unionFeatures(SimpleFeatureCollection inputFeatures) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                geometries.add(geometry);
            }
        } finally {
            if (featureIter != null)
                featureIter.close();
        }

        if (geometries.size() == 0) {
            return null;
        } else if (geometries.size() == 1) {
            return geometries.iterator().next();
        }

        com.vividsolutions.jts.operation.union.CascadedPolygonUnion unionOp = null;
        unionOp = new CascadedPolygonUnion(geometries);
        return unionOp.union();
    }
}