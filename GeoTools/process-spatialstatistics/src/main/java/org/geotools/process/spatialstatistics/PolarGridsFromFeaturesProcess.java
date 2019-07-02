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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.RenderingProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.RadialType;
import org.geotools.process.spatialstatistics.operations.PolarGridsOperation;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.util.ProgressListener;

/**
 * Creates a radial polar grids from features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolarGridsFromFeaturesProcess extends AbstractStatisticsProcess implements
        RenderingProcess {
    protected static final Logger LOGGER = Logging.getLogger(PolarGridsFromFeaturesProcess.class);

    public PolarGridsFromFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection origin, String radius,
            RadialType radialType, Integer sides, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PolarGridsFromFeaturesProcessFactory.origin.key, origin);
        map.put(PolarGridsFromFeaturesProcessFactory.radius.key, radius);
        map.put(PolarGridsFromFeaturesProcessFactory.radialType.key, radialType);
        map.put(PolarGridsFromFeaturesProcessFactory.sides.key, sides);

        Process process = new PolarGridsFromFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(PolarGridsFromFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection origin = (SimpleFeatureCollection) Params.getValue(input,
                PolarGridsFromFeaturesProcessFactory.origin, null);
        String radius = (String) Params.getValue(input,
                PolarGridsFromFeaturesProcessFactory.radius, null);
        if (origin == null || radius == null || radius.length() == 0) {
            throw new NullPointerException("origin, radius parameters required");
        }
        RadialType radialType = (RadialType) Params.getValue(input,
                PolarGridsFromFeaturesProcessFactory.radialType,
                PolarGridsFromFeaturesProcessFactory.radialType.sample);
        Integer sides = (Integer) Params.getValue(input,
                PolarGridsFromFeaturesProcessFactory.sides,
                PolarGridsFromFeaturesProcessFactory.sides.sample);

        // start process
        String[] arrDistance = radius.split(",");
        double[] bufferRadius = new double[arrDistance.length];
        for (int k = 0; k < arrDistance.length; k++) {
            try {
                bufferRadius[k] = Double.parseDouble(arrDistance[k].trim());
            } catch (NumberFormatException nfe) {
                throw new ProcessException(nfe);
            }
        }

        SimpleFeatureCollection resultFc = null;
        try {
            PolarGridsOperation operation = new PolarGridsOperation();
            resultFc = operation.execute(origin, bufferRadius, sides, radialType);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PolarGridsFromFeaturesProcessFactory.RESULT.key, resultFc);
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
        String radius = (String) input.get(PolarGridsFromFeaturesProcessFactory.radius.key);
        if (radius == null || radius.trim().length() == 0) {
            return targetQuery;
        }

        String[] splits = radius.split(",");
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