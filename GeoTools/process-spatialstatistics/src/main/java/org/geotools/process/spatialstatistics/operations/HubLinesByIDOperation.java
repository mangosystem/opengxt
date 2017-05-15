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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by id.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class HubLinesByIDOperation extends AbstractHubLinesOperation {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByIDOperation.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures, String spokeIdField, boolean useCentroid,
            boolean preserveAttributes, double maximumDistance) throws IOException {

        this.setPreserveAttributes(preserveAttributes);
        this.setMaximumDistance(maximumDistance);
        this.setUseCentroid(useCentroid);

        return execute(hubFeatures, hubIdField, spokeFeatures, spokeIdField);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection hubFeatures, String hubIdField,
            SimpleFeatureCollection spokeFeatures, String spokeIdField) throws IOException {
        SimpleFeatureType spokeSchema = spokeFeatures.getSchema();
        CoordinateReferenceSystem crs = spokeSchema.getCoordinateReferenceSystem();

        SimpleFeatureType featureType = null;
        if (preserveAttributes) {
            featureType = FeatureTypes.build(spokeSchema, TYPE_NAME, LineString.class);
        } else {
            String geomName = spokeSchema.getGeometryDescriptor().getLocalName();
            featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName, LineString.class, crs);
            featureType = FeatureTypes.add(featureType, spokeSchema.getDescriptor(spokeIdField));
        }

        AttributeDescriptor hubIdDesc = hubFeatures.getSchema().getDescriptor(hubIdField);
        featureType = FeatureTypes.add(featureType, hubIdDesc);
        featureType = FeatureTypes.add(featureType, HUB_DIST, Double.class, 38);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // check coordinate reference system
        CoordinateReferenceSystem crsT = spokeFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = hubFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            hubFeatures = new ReprojectFeatureCollection(hubFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        SimpleFeatureIterator hubIter = hubFeatures.features();
        try {
            while (hubIter.hasNext()) {
                SimpleFeature hubFeature = hubIter.next();
                Geometry hubGeom = (Geometry) hubFeature.getDefaultGeometry();

                Object hubID = hubFeature.getAttribute(hubIdField);
                Filter filter = ff.equals(ff.property(spokeIdField), ff.literal(hubID));
                if (hubID == null) {
                    filter = ff.isNull(ff.property(spokeIdField));
                }

                SimpleFeatureIterator spokeIter = spokeFeatures.subCollection(filter).features();
                try {
                    while (spokeIter.hasNext()) {
                        SimpleFeature spokeFeature = spokeIter.next();
                        Geometry spokeGeom = (Geometry) spokeFeature.getDefaultGeometry();

                        // create line: direction = hub --> spoke
                        Geometry hubLine = getShortestLine(hubGeom, spokeGeom, useCentroid);
                        double distance = hubLine.getLength();
                        if (distance == 0 || this.maximumDistance < distance) {
                            continue;
                        }

                        // insert features
                        SimpleFeature newFeature = featureWriter.buildFeature();
                        if (preserveAttributes) {
                            featureWriter.copyAttributes(spokeFeature, newFeature, false);
                        } else {
                            Object spokeID = spokeFeature.getAttribute(spokeIdField);
                            newFeature.setAttribute(spokeIdField, spokeID);
                        }

                        newFeature.setDefaultGeometry(hubLine);
                        newFeature.setAttribute(hubIdField, hubID);
                        newFeature.setAttribute(HUB_DIST, distance);

                        featureWriter.write(newFeature);
                    }
                } finally {
                    spokeIter.close();
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(hubIter);
        }

        return featureWriter.getFeatureCollection();
    }
}
