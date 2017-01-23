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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Finds duplicated geometries(not attributes) in features and removes them.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class DeleteDuplicateFeaturesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging
            .getLogger(DeleteDuplicateFeaturesOperation.class);

    public DeleteDuplicateFeaturesOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features) throws IOException {
        SimpleFeatureType schema = features.getSchema();

        STRtree spatialIndex = buildSpatialIndex(features);
        List<String> processedMap = new ArrayList<String>();

        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                String featureID = feature.getID();
                if (processedMap.contains(featureID)) {
                    continue;
                }

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                PreparedGeometry prepared = PreparedGeometryFactory.prepare(geometry);

                // find coincident events
                for (@SuppressWarnings("unchecked")
                Iterator<NearFeature> iter = (Iterator<NearFeature>) spatialIndex.query(
                        geometry.getEnvelopeInternal()).iterator(); iter.hasNext();) {
                    NearFeature sample = iter.next();
                    if (processedMap.contains(sample.id) || sample.id.equals(featureID)) {
                        continue;
                    }

                    if (prepared.disjoint(sample.location)) {
                        continue;
                    }

                    if (sample.location.equals(geometry)) {
                        processedMap.add(sample.id);
                    }
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
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

    private STRtree buildSpatialIndex(SimpleFeatureCollection points) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = points.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                NearFeature near = new NearFeature(geometry, feature.getID());
                spatialIndex.insert(geometry.getEnvelopeInternal(), near);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class NearFeature {

        public Geometry location;

        public String id;

        public NearFeature(Geometry location, String id) {
            this.location = location;
            this.id = id;
        }
    }

}
