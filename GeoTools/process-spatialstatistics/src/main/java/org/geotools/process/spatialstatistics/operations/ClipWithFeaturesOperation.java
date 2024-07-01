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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ClipWithGeometryFeatureCollection;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Extracts input features that overlay the clip polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class ClipWithFeaturesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ClipWithFeaturesOperation.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection clipFeatures) throws IOException {
        SimpleFeatureType featureType = buildTargetSchema(inputFeatures.getSchema());

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // check coordinate reference system
        CoordinateReferenceSystem crsT = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = clipFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            clipFeatures = new ReprojectFeatureCollection(clipFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // using SpatialIndexFeatureCollection
        inputFeatures = DataUtils.toSpatialIndexFeatureCollection(inputFeatures,
                clipFeatures.getBounds());

        SimpleFeatureIterator clipIter = clipFeatures.features();
        try {
            String geomName = featureType.getGeometryDescriptor().getLocalName();
            while (clipIter.hasNext()) {
                SimpleFeature feature = clipIter.next();
                Geometry clipGeometry = (Geometry) feature.getDefaultGeometry();

                Filter filter = ff.bbox(ff.property(geomName), JTS.toEnvelope(clipGeometry));
                featureWriter.write(new ClipWithGeometryFeatureCollection(inputFeatures
                        .subCollection(filter), clipGeometry));
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(clipIter);
        }

        return featureWriter.getFeatureCollection();
    }

    /**
     * When clipping lines and polygons can turn into multilines and multipolygons
     * 
     * @reference org.geotools.process.vector.ClipProcess.java
     */
    private SimpleFeatureType buildTargetSchema(SimpleFeatureType schema) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                Class<?> binding = ad.getType().getBinding();
                if (Point.class.isAssignableFrom(binding)
                        || GeometryCollection.class.isAssignableFrom(binding)) {
                    tb.add(ad);
                } else {
                    Class<?> target;
                    if (LineString.class.isAssignableFrom(binding)) {
                        target = MultiLineString.class;
                    } else if (Polygon.class.isAssignableFrom(binding)) {
                        target = MultiPolygon.class;
                    } else {
                        throw new RuntimeException("Don't know how to handle geometries of type "
                                + binding.getCanonicalName());
                    }
                    tb.minOccurs(ad.getMinOccurs());
                    tb.maxOccurs(ad.getMaxOccurs());
                    tb.nillable(ad.isNillable());
                    tb.add(ad.getLocalName(), target, gd.getCoordinateReferenceSystem());
                }
            } else {
                tb.add(ad);
            }
        }
        tb.setName(schema.getName());
        return tb.buildFeatureType();
    }
}