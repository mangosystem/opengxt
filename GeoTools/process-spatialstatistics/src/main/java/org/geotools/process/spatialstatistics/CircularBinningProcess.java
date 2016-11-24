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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.RenderingProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.pattern.CircularBinningOperation;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Performs rectangular binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CircularBinningProcess extends AbstractStatisticsProcess implements RenderingProcess {
    protected static final Logger LOGGER = Logging.getLogger(CircularBinningProcess.class);

    private boolean started = false;

    public CircularBinningProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression weight, ReferencedEnvelope bbox, Double radius, Boolean validGrid,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CircularBinningProcessFactory.features.key, features);
        map.put(CircularBinningProcessFactory.weight.key, weight);
        map.put(CircularBinningProcessFactory.bbox.key, bbox);
        map.put(CircularBinningProcessFactory.radius.key, radius);
        map.put(CircularBinningProcessFactory.validGrid.key, validGrid);

        Process process = new CircularBinningProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(CircularBinningProcessFactory.RESULT.key);
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

        try {
            SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                    CircularBinningProcessFactory.features, null);
            if (features == null) {
                throw new NullPointerException("features parameter required");
            }

            Expression weight = (Expression) Params.getValue(input,
                    CircularBinningProcessFactory.weight, null);
            ReferencedEnvelope bbox = (ReferencedEnvelope) Params.getValue(input,
                    CircularBinningProcessFactory.bbox, null);
            Double radius = (Double) Params.getValue(input, CircularBinningProcessFactory.radius,
                    null);
            Boolean validGrid = (Boolean) Params.getValue(input,
                    CircularBinningProcessFactory.validGrid,
                    CircularBinningProcessFactory.validGrid.sample);

            // start process
            if (bbox == null || bbox.isEmpty()) {
                bbox = features.getBounds();
            }

            if (radius == null || radius <= 0) {
                radius = (Math.min(bbox.getWidth(), bbox.getHeight()) / 250.0d) / 2.0d;
                LOGGER.log(Level.WARNING, "The default width / height is " + radius);
            }

            CircularBinningOperation process = new CircularBinningOperation();
            process.setOnlyValidGrid(validGrid);
            SimpleFeatureCollection resultFc = process.execute(features, weight, bbox, radius);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(CircularBinningProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
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
        Double radius = (Double) input.get(CircularBinningProcessFactory.radius.key);
        if (radius == null || radius <= 0) {
            return targetQuery;
        }

        if (radius > 0) {
            targetQuery.setFilter(expandBBox(targetQuery.getFilter(), radius));
            targetQuery.setProperties(null);
            targetQuery.getHints().put(Hints.GEOMETRY_DISTANCE, 0.0);
        }

        return targetQuery;
    }
}