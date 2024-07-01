/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.ForceDimensionFeatureCollection;
import org.geotools.process.spatialstatistics.util.GeometryDimensions.DimensionType;
import org.geotools.util.logging.Logging;

/**
 * Force the coordinate dimension(XY, XYZ, XYM, XYZM) of a geometry
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ForceDimensionProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ForceDimensionProcess.class);

    public ForceDimensionProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            DimensionType dimension, ProgressListener monitor) {
        return ForceDimensionProcess.process(inputFeatures, dimension, null, null, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            DimensionType dimension, Expression zField, ProgressListener monitor) {
        return ForceDimensionProcess.process(inputFeatures, dimension, zField, null, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            DimensionType dimension, Expression zField, Expression mField,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ForceDimensionProcessFactory.inputFeatures.key, inputFeatures);
        map.put(ForceDimensionProcessFactory.dimension.key, dimension);
        map.put(ForceDimensionProcessFactory.zField.key, zField);
        map.put(ForceDimensionProcessFactory.mField.key, mField);

        Process process = new ForceDimensionProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(ForceDimensionProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                ForceDimensionProcessFactory.inputFeatures, null);
        DimensionType dimension = (DimensionType) Params.getValue(input,
                ForceDimensionProcessFactory.dimension,
                ForceDimensionProcessFactory.dimension.sample);
        Expression zField = (Expression) Params.getValue(input, ForceDimensionProcessFactory.zField,
                null);
        Expression mField = (Expression) Params.getValue(input, ForceDimensionProcessFactory.mField,
                null);
        if (features == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFC = new ForceDimensionFeatureCollection(features, dimension,
                zField, mField);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ForceDimensionProcessFactory.RESULT.key, resultFC);
        return resultMap;
    }
}
