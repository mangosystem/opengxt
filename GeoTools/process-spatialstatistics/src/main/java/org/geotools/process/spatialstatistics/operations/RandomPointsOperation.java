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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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

    com.vividsolutions.jts.shape.random.RandomPointsBuilder builder;

    CoordinateReferenceSystem crs;

    public RandomPointsOperation(ReferencedEnvelope extent) {
        builder = new RandomPointsBuilder(gf);
        crs = extent.getCoordinateReferenceSystem();
        builder.setExtent(extent);
    }

    public RandomPointsOperation(SimpleFeatureCollection boundsSource) {
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
    }

    public SimpleFeatureCollection execute(int pointCount) throws IOException {
        builder.setNumPoints(pointCount);

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("RandomPoints");
        typeBuilder.add("geom", Point.class, crs);
        typeBuilder.add("weight", Integer.class);
        SimpleFeatureType schema = typeBuilder.buildFeatureType();

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            Geometry multiPoints = builder.getGeometry();
            for (int i = 0; i < multiPoints.getNumGeometries(); i++) {
                Point point = (Point) multiPoints.getGeometryN(i);

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature(null);
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