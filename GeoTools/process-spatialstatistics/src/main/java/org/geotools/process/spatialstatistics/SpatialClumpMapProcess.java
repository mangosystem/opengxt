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

import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.data.Query;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.RenderingProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.transformation.SpatialClumpFeatureCollection;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Creates a spatial clump map using point features and radius expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialClumpMapProcess extends AbstractStatisticsProcess implements RenderingProcess {
    protected static final Logger LOGGER = Logging.getLogger(SpatialClumpMapProcess.class);

    public SpatialClumpMapProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression radius, Integer quadrantSegments, ProgressListener monitor) {
        return SpatialClumpMapProcess.process(inputFeatures, radius, DistanceUnit.Default,
                quadrantSegments, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Expression radius, DistanceUnit radiusUnit, Integer quadrantSegments,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SpatialClumpMapProcessFactory.inputFeatures.key, inputFeatures);
        map.put(SpatialClumpMapProcessFactory.radius.key, radius);
        map.put(SpatialClumpMapProcessFactory.radiusUnit.key, radiusUnit);
        map.put(SpatialClumpMapProcessFactory.quadrantSegments.key, quadrantSegments);

        Process process = new SpatialClumpMapProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(SpatialClumpMapProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                SpatialClumpMapProcessFactory.inputFeatures, null);
        Expression radius = (Expression) Params.getValue(input,
                SpatialClumpMapProcessFactory.radius, null);
        DistanceUnit radiusUnit = (DistanceUnit) Params.getValue(input,
                SpatialClumpMapProcessFactory.radiusUnit,
                SpatialClumpMapProcessFactory.radiusUnit.sample);
        Integer quadrantSegments = (Integer) Params.getValue(input,
                SpatialClumpMapProcessFactory.quadrantSegments,
                SpatialClumpMapProcessFactory.quadrantSegments.sample);
        if (inputFeatures == null || radius == null) {
            throw new NullPointerException("All parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = DataUtilities.simple(new SpatialClumpFeatureCollection(
                inputFeatures, radius, radiusUnit, quadrantSegments));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(SpatialClumpMapProcessFactory.RESULT.key, resultFc);
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
        Expression expression = (Expression) input.get(SpatialClumpMapProcessFactory.radius.key);
        if (expression == null) {
            return targetQuery;
        }

        if (expression instanceof Literal) {
            double queryBuffer = 0d;
            try {
                Object value = ((Literal) expression).getValue();
                if (Number.class.isAssignableFrom(value.getClass())) {
                    queryBuffer = ((Number) value).doubleValue();
                }
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.WARNING, nfe.getMessage());
            }

            if (queryBuffer > 0) {
                targetQuery.setFilter(expandBBox(targetQuery.getFilter(), queryBuffer));
                targetQuery.setProperties(null);
                targetQuery.getHints().put(Hints.GEOMETRY_DISTANCE, 0.0);
            }
        }

        return targetQuery;
    }
}