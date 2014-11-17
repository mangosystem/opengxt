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
package org.geotools.process.spatialstatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.text.Text;
import org.geotools.util.Converters;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;

/**
 * Collect Event combines coincident points. It converts event data, such as crime or disease incidents, to weighted point data.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CollectEventsProcess extends AbstractProcess {
    protected static final Logger LOGGER = Logging.getLogger(CollectEventsProcess.class);

    private boolean started = false;

    public CollectEventsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String countField, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CollectEventsProcessFactory.inputFeatures.key, inputFeatures);
        map.put(CollectEventsProcessFactory.countField.key, countField);

        Process process = new CollectEventsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(CollectEventsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, CollectEventsProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameter required");
            }

            String countField = (String) Params.getValue(input,
                    CollectEventsProcessFactory.countField,
                    CollectEventsProcessFactory.countField.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            CollectEventsOperation operation = new CollectEventsOperation();
            SimpleFeatureCollection resultFc = operation.execute(inputFeatures, countField);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(CollectEventsProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    public class CollectEventsOperation {

        public static final double XY_TOL = 0.1;

        public SimpleFeatureCollection execute(SimpleFeatureCollection points, String countField) {
            String typeName = points.getSchema().getTypeName();
            SimpleFeatureType schema = FeatureTypes.build(points.getSchema(), typeName);
            if (countField == null || countField.isEmpty()) {
                countField = CollectEventsProcessFactory.countField.sample.toString();
            }
            schema = FeatureTypes.add(schema, countField, Integer.class);
            Class<?> outputBinding = schema.getDescriptor(countField).getType().getBinding();

            KdTree kdTree = buildIndex(points);
            List<String> processedMap = new ArrayList<String>();

            ListFeatureCollection featureCollection = new ListFeatureCollection(schema);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
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
                    builder.init(feature);
                    SimpleFeature newFeature = builder.buildFeature(featureID);
                    Object countVal = Converters.convert(featureCount, outputBinding);
                    newFeature.setAttribute(countField, countVal);
                    featureCollection.add(newFeature);
                    processedMap.add(featureID);
                }
            } finally {
                featureIter.close();
            }

            return featureCollection;
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

}
