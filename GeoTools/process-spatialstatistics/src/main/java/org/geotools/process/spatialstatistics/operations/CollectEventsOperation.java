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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * Collect Event combines coincident points.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class CollectEventsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(CollectEventsOperation.class);

    static final String COUNT_FIELD = "icount";

    private double XY_TOL = 0.1;

    public SimpleFeatureCollection execute(SimpleFeatureCollection points, String countField)
            throws IOException {
        String typeName = points.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(points.getSchema(), typeName);
        if (countField == null || countField.isEmpty()) {
            countField = COUNT_FIELD;
        }
        schema = FeatureTypes.add(schema, countField, Integer.class);
        Class<?> outputBinding = schema.getDescriptor(countField).getType().getBinding();

        KdTree kdTree = buildIndex(points);
        List<String> processedMap = new ArrayList<String>();

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = points.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                String featureID = feature.getID();
                if (processedMap.contains(featureID)) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Geometry buffered = geometry.buffer(XY_TOL);

                int featureCount = 1;
                @SuppressWarnings("unchecked")
                List<KdNode> nodes = kdTree.query(buffered.getEnvelopeInternal());
                if (nodes.size() > 0) {
                    Coordinate coordinate = geometry.getCoordinate();
                    for (KdNode node : nodes) {
                        String fid = node.getData().toString();
                        if (processedMap.contains(fid) || fid.equals(featureID)) {
                            continue;
                        }

                        double dist = coordinate.distance(node.getCoordinate());
                        if (dist > XY_TOL) {
                            continue;
                        }
                        featureCount++;
                        processedMap.add(fid);
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature(featureID);
                featureWriter.copyAttributes(feature, newFeature, true);
                Object countVal = Converters.convert(featureCount, outputBinding);
                newFeature.setAttribute(countField, countVal);

                featureWriter.write(newFeature);
                processedMap.add(featureID);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private KdTree buildIndex(SimpleFeatureCollection points) {
        KdTree spatialIndex = new KdTree(0.0d);
        SimpleFeatureIterator featureIter = points.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                spatialIndex.insert(geometry.getCoordinate(), feature.getID());
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

}
