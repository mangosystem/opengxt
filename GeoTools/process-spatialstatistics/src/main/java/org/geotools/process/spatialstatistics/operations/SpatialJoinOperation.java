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
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.SpatialJoinType;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * SpatialJoin : One by One
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialJoinOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(SpatialJoinOperation.class);

    private double searchRadius = 0.0d;

    public double getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection joinFeatures, SpatialJoinType joinType) throws IOException {
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(inputFeatures.getSchema(), typeName);

        List<String> propertyList = new ArrayList<String>();
        SimpleFeatureType joinSchema = joinFeatures.getSchema();
        for (AttributeDescriptor desc : joinSchema.getAttributeDescriptors()) {
            if (desc instanceof GeometryDescriptor) {
                continue;
            } else {
                // check duplicate fields
                if (FeatureTypes.existProeprty(schema, desc.getLocalName())) {
                    continue;
                }
                schema = FeatureTypes.add(schema, desc);
                propertyList.add(desc.getLocalName());
            }
        }

        // check CRS
        CoordinateReferenceSystem aCrs = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem bCrs = joinFeatures.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(aCrs, bCrs) && bCrs != null) {
            // reproject joinFeatures to inputFeatures CRS
            joinFeatures = new ReprojectingFeatureCollection(joinFeatures, aCrs);
        }

        STRtree spatialIndex = loadFeatures(joinFeatures);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry source = (Geometry) feature.getDefaultGeometry();

                // find nearest features
                SimpleFeature joinFeature = (SimpleFeature) spatialIndex.nearestNeighbour(
                        source.getEnvelopeInternal(), feature, new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                SimpleFeature s1 = (SimpleFeature) item1.getItem();
                                SimpleFeature s2 = (SimpleFeature) item2.getItem();

                                Geometry g1 = (Geometry) s1.getDefaultGeometry();
                                Geometry g2 = (Geometry) s2.getDefaultGeometry();
                                return g1.distance(g2);
                            }
                        });

                Geometry target = (Geometry) joinFeature.getDefaultGeometry();
                double distance = source.distance(target);
                if (searchRadius > 0 && searchRadius < distance) {
                    joinFeature = null;
                }

                // create & insert feature
                if (joinType == SpatialJoinType.OnlyMatchingRecord && joinFeature == null) {
                    continue;
                }

                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                if (joinFeature != null) {
                    for (String name : propertyList) {
                        newFeature.setAttribute(name, joinFeature.getAttribute(name));
                    }
                }
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private STRtree loadFeatures(SimpleFeatureCollection joinFeatures) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = joinFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                spatialIndex.insert(geometry.getEnvelopeInternal(), feature);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }
}
