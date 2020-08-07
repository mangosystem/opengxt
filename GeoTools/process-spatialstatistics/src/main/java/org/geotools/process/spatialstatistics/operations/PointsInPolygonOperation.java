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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Points In Polygon Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsInPolygonOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PointsInPolygonOperation.class);

    public static final String AGG_FIELD = "val";

    public PointsInPolygonOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygonFeatures,
            SimpleFeatureCollection pointFeatures) throws IOException {
        return execute(polygonFeatures, pointFeatures, null);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygonFeatures,
            SimpleFeatureCollection pointFeatures, Expression weight) throws IOException {
        // check coordinate reference system
        CoordinateReferenceSystem crsT = polygonFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = pointFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            pointFeatures = new ReprojectFeatureCollection(pointFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // use SpatialIndexFeatureCollection
        pointFeatures = DataUtils.toSpatialIndexFeatureCollection(pointFeatures);

        SimpleFeatureType schema = polygonFeatures.getSchema();
        schema = FeatureTypes.add(schema, AGG_FIELD, Double.class);

        final String the_geom = pointFeatures.getSchema().getGeometryDescriptor().getLocalName();
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        SimpleFeatureIterator featureIter = polygonFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Filter filter = getIntersectsFilter(the_geom, geometry);

                double aggregated = 0;
                if (weight == null) {
                    aggregated = pointFeatures.subCollection(filter).size();
                } else {
                    SimpleFeatureIterator pointIter = null;
                    try {
                        pointIter = pointFeatures.subCollection(filter).features();
                        while (pointIter.hasNext()) {
                            SimpleFeature pointFeature = pointIter.next();
                            Double evaluated = weight.evaluate(pointFeature, Double.class);
                            if (evaluated == null || evaluated.isNaN() || evaluated.isInfinite()) {
                                continue;
                            }
                            aggregated += evaluated;
                        }
                    } finally {
                        pointIter.close();
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                newFeature.setAttribute(AGG_FIELD, aggregated);
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

}
