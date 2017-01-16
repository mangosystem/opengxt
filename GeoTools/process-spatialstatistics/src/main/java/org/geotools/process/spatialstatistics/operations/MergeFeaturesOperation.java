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
import java.util.Collection;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Combines multiple input features of the same data type into a single, new output features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class MergeFeaturesOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(MergeFeaturesOperation.class);

    static final String outputName = "Merge";

    public MergeFeaturesOperation() {

    }

    public SimpleFeatureCollection execute(Collection<SimpleFeatureCollection> featureList,
            SimpleFeatureCollection template) throws IOException {
        if (featureList.size() == 0) {
            throw new ProcessException("featureList parameter is empty!");
        }

        // prepare feature type
        SimpleFeatureType destSchema = null;
        if (template != null) {
            destSchema = FeatureTypes.build(template, outputName);
        } else {
            SimpleFeatureType inputSchema = featureList.iterator().next().getSchema();
            destSchema = FeatureTypes.build(inputSchema, outputName);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(destSchema);
        try {
            for (SimpleFeatureCollection collection : featureList) {
                SimpleFeatureType inputSchema = collection.getSchema();

                SimpleFeatureIterator featureIter = collection.features();
                try {
                    while (featureIter.hasNext()) {
                        SimpleFeature feature = featureIter.next();

                        // create feature and set geometry
                        SimpleFeature newFeature = featureWriter.buildFeature();
                        for (AttributeDescriptor ad : destSchema.getAttributeDescriptors()) {
                            if (ad instanceof GeometryDescriptor
                                    || inputSchema.indexOf(ad.getName()) == -1) {
                                continue;
                            }
                            newFeature.setAttribute(ad.getName(),
                                    feature.getAttribute(ad.getName()));
                        }
                        newFeature.setDefaultGeometry(feature.getDefaultGeometry());
                        featureWriter.write(newFeature);
                    }
                } finally {
                    featureIter.close();
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}