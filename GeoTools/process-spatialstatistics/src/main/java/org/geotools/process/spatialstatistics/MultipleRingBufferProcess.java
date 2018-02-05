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
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.RenderingProcess;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.transformation.MultipleBufferFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
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
public class MultipleRingBufferProcess extends AbstractStatisticsProcess implements
        RenderingProcess {
    protected static final Logger LOGGER = Logging.getLogger(MultipleRingBufferProcess.class);

    static final String bufferField = "distance";

    public MultipleRingBufferProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String distances, Boolean outsideOnly, Boolean dissolve, ProgressListener monitor) {
        return MultipleRingBufferProcess.process(inputFeatures, distances, DistanceUnit.Default,
                outsideOnly, dissolve, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String distances, DistanceUnit distanceUnit, Boolean outsideOnly, Boolean dissolve,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MultipleRingBufferProcessFactory.inputFeatures.key, inputFeatures);
        map.put(MultipleRingBufferProcessFactory.distances.key, distances);
        map.put(MultipleRingBufferProcessFactory.distanceUnit.key, distanceUnit);
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
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                MultipleRingBufferProcessFactory.inputFeatures, null);

        String distances = (String) Params.getValue(input,
                MultipleRingBufferProcessFactory.distances, null);
        if (inputFeatures == null || distances == null || distances.trim().length() == 0) {
            throw new NullPointerException("inputFeatures, distances parameters required");
        }

        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                MultipleRingBufferProcessFactory.distanceUnit,
                MultipleRingBufferProcessFactory.distanceUnit.sample);

        Boolean outsideOnly = (Boolean) Params.getValue(input,
                MultipleRingBufferProcessFactory.outsideOnly,
                MultipleRingBufferProcessFactory.outsideOnly.sample);
        Boolean dissolve = (Boolean) Params.getValue(input,
                MultipleRingBufferProcessFactory.dissolve,
                MultipleRingBufferProcessFactory.dissolve.sample);

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

        SimpleFeatureCollection resultFc = new MultipleBufferFeatureCollection(inputFeatures,
                bufferDistance, distanceUnit, outsideOnly);

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

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(MultipleRingBufferProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

    /**
     * Given a target query and a target grid geometry returns the grid geometry to be used to read the input data of the process involved in
     * rendering. This method will be called only if the input data is a grid coverage or a grid coverage reader
     */
    @Override
    public GridGeometry invertGridGeometry(Map<String, Object> input, Query targetQuery,
            GridGeometry targetGridGeometry) throws ProcessException {
        return targetGridGeometry;
    }

    /**
     * Given a target query and a target grid geometry returns the query to be used to read the input data of the process involved in rendering. This
     * method will be called only if the input data is a feature collection.
     */

    @Override
    public Query invertQuery(Map<String, Object> input, Query targetQuery,
            GridGeometry targetGridGeometry) throws ProcessException {
        String distances = (String) input.get(MultipleRingBufferProcessFactory.distances.key);
        if (distances == null || distances.trim().length() == 0) {
            return targetQuery;
        }

        String[] splits = distances.split(",");
        double queryBuffer = 0d;
        for (int k = 0; k < splits.length; k++) {
            try {
                double distance = Double.parseDouble(splits[k].trim());
                queryBuffer = Math.max(distance, queryBuffer);
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.WARNING, nfe.getMessage());
            }
        }

        if (queryBuffer > 0) {
            targetQuery.setFilter(expandBBox(targetQuery.getFilter(), queryBuffer));
            targetQuery.setProperties(null);
            targetQuery.getHints().put(Hints.GEOMETRY_DISTANCE, 0.0);
        }

        return targetQuery;
    }
}
