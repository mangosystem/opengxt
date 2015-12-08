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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.GXTSimpleFeatureCollection;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Creates a new features of buffer features using a set of buffer distances.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MultipleRingBufferProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(MultipleRingBufferProcess.class);

    private boolean started = false;

    static final String bufferField = "distance";

    public MultipleRingBufferProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String distances, Boolean outsideOnly, Boolean dissolve, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MultipleRingBufferProcessFactory.inputFeatures.key, inputFeatures);
        map.put(MultipleRingBufferProcessFactory.distances.key, distances);
        map.put(MultipleRingBufferProcessFactory.outsideOnly.key, outsideOnly);
        map.put(MultipleRingBufferProcessFactory.dissolve.key, dissolve);

        Process process = new MultipleRingBufferProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(MultipleRingBufferProcessFactory.RESULT.key);
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
                    input, MultipleRingBufferProcessFactory.inputFeatures, null);
            String distances = (String) Params.getValue(input,
                    MultipleRingBufferProcessFactory.distances, null);
            if (inputFeatures == null || distances == null || distances.trim().length() == 0) {
                throw new NullPointerException("inputFeatures, distances parameters required");
            }

            Boolean outsideOnly = (Boolean) Params.getValue(input,
                    MultipleRingBufferProcessFactory.outsideOnly,
                    MultipleRingBufferProcessFactory.outsideOnly.sample);
            Boolean dissolve = (Boolean) Params.getValue(input,
                    MultipleRingBufferProcessFactory.dissolve,
                    MultipleRingBufferProcessFactory.dissolve.sample);

            monitor.setTask(Text.text("Processing " + this.getClass().getSimpleName()));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            String[] arrDistance = distances.split(",");
            double[] bufferDistance = new double[arrDistance.length];
            for (int k = 0; k < arrDistance.length; k++) {
                try {
                    bufferDistance[k] = Double.parseDouble(arrDistance[k].trim());
                } catch (NumberFormatException nfe) {
                    throw new NumberFormatException(nfe.getMessage());
                }
            }

            SimpleFeatureCollection resultFc = new MultipleBufferedFeatureCollection(inputFeatures,
                    bufferDistance, outsideOnly);

            if (dissolve) {
                Map<Double, List<Geometry>> map = new TreeMap<Double, List<Geometry>>();
                SimpleFeatureIterator featureIter = resultFc.features();
                try {
                    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
                    Expression expression = ff.property(bufferField);
                    while (featureIter.hasNext()) {
                        SimpleFeature feature = featureIter.next();
                        Geometry geometry = (Geometry) feature.getDefaultGeometry();
                        Double distance = expression.evaluate(feature, Double.class);
                        if (!map.containsKey(distance)) {
                            map.put(distance, new ArrayList<Geometry>());
                        }
                        map.get(distance).add(geometry);
                    }
                } finally {
                    featureIter.close();
                }

                // inert features
                SimpleFeatureType sfType = inputFeatures.getSchema();
                String typeName = sfType.getTypeName();
                CoordinateReferenceSystem crs = sfType.getCoordinateReferenceSystem();
                String geomField = sfType.getGeometryDescriptor().getLocalName();
                SimpleFeatureType schema = FeatureTypes.getDefaultType(typeName, geomField,
                        Polygon.class, crs);
                schema = FeatureTypes.add(schema, bufferField, Double.class, 19);

                ListFeatureCollection unionFc = new ListFeatureCollection(schema);
                SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);

                int featureID = 0;
                for (Map.Entry<Double, List<Geometry>> entrySet : map.entrySet()) {
                    Geometry unionGeometry = CascadedPolygonUnion.union(entrySet.getValue());

                    SimpleFeature unionFeautre = fb.buildFeature(Integer.toString(++featureID));
                    unionFeautre.setDefaultGeometry(unionGeometry);
                    unionFeautre.setAttribute(bufferField, entrySet.getKey());
                    unionFc.add(unionFeautre);
                }

                resultFc = unionFc;
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(MultipleRingBufferProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            throw new ProcessException(eek);
        } finally {
            monitor.dispose();
        }
    }

    /**
     * Wrapper that will trigger the buffer computation as features are requested
     */
    static class MultipleBufferedFeatureCollection extends GXTSimpleFeatureCollection {

        private double[] distances;

        private Boolean outsideOnly = Boolean.TRUE;

        private SimpleFeatureType schema;

        public MultipleBufferedFeatureCollection(SimpleFeatureCollection delegate,
                double[] distances, Boolean outsideOnly) {
            super(delegate);

            Arrays.sort(distances);
            this.distances = distances;
            this.outsideOnly = outsideOnly;

            String typeName = delegate.getSchema().getTypeName();
            this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
            this.schema = FeatureTypes.add(schema, bufferField, Double.class, 19);
        }

        @Override
        public SimpleFeatureIterator features() {
            return new BufferedFeatureIterator(delegate, getSchema(), distances, outsideOnly);
        }

        @Override
        public SimpleFeatureType getSchema() {
            return schema;
        }

        @Override
        public ReferencedEnvelope getBounds() {
            ReferencedEnvelope bounds = delegate.getBounds();
            bounds.expandBy(distances[distances.length - 1]);
            return bounds;
        }

        @Override
        public int size() {
            return delegate.size() * distances.length;
        }

        /**
         * Buffers each feature as we scroll over the collection
         */
        static class BufferedFeatureIterator implements SimpleFeatureIterator {
            private SimpleFeatureIterator delegate;

            private double[] distances;

            private Boolean outsideOnly = Boolean.TRUE;

            private int bufferIndex = 0;

            private int featureID = 0;

            private SimpleFeatureBuilder builder;

            private SimpleFeature nextFeature = null;

            private SimpleFeature origFeature = null;

            public BufferedFeatureIterator(SimpleFeatureCollection delegate,
                    SimpleFeatureType schema, double[] distances, Boolean outsideOnly) {
                this.delegate = delegate.features();

                this.bufferIndex = 0;
                this.distances = distances;
                this.outsideOnly = outsideOnly;
                this.builder = new SimpleFeatureBuilder(schema);
            }

            public void close() {
                delegate.close();
            }

            public boolean hasNext() {
                while ((nextFeature == null && delegate.hasNext())
                        || (nextFeature == null && !delegate.hasNext() && bufferIndex > 0)) {
                    if (bufferIndex == 0) {
                        origFeature = delegate.next();
                    }

                    // buffer geometry
                    Geometry orig = (Geometry) origFeature.getDefaultGeometry();
                    Geometry buff = orig.buffer(distances[bufferIndex], 24);
                    if (outsideOnly && bufferIndex > 0) {
                        buff = buff.difference(orig.buffer(distances[bufferIndex - 1], 24));
                    }

                    // create feature
                    nextFeature = builder.buildFeature(Integer.toString(++featureID));
                    transferAttribute(origFeature, nextFeature);
                    nextFeature.setDefaultGeometry(buff);
                    nextFeature.setAttribute(bufferField, distances[bufferIndex]);

                    builder.reset();
                    bufferIndex++;

                    if (bufferIndex >= distances.length) {
                        bufferIndex = 0;
                        origFeature = null;
                    }
                }
                return nextFeature != null;
            }

            public SimpleFeature next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException("hasNext() returned false!");
                }
                SimpleFeature result = nextFeature;
                nextFeature = null;
                return result;
            }
        }
    }
}
