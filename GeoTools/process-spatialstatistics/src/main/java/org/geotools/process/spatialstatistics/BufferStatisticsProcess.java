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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.PointStatisticsOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.ProgressListener;

/**
 * Perform point in polygon analysis
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BufferStatisticsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(BufferStatisticsProcess.class);

    public BufferStatisticsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection pointFeatures, String countField, String statisticsFields,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(BufferStatisticsProcessFactory.inputFeatures.key, inputFeatures);
        map.put(BufferStatisticsProcessFactory.pointFeatures.key, pointFeatures);
        map.put(BufferStatisticsProcessFactory.countField.key, countField);
        map.put(BufferStatisticsProcessFactory.statisticsFields.key, statisticsFields);

        Process process = new BufferStatisticsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(BufferStatisticsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                BufferStatisticsProcessFactory.inputFeatures, null);
        SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(input,
                BufferStatisticsProcessFactory.pointFeatures, null);
        Double distance = (Double) Params.getValue(input, BufferStatisticsProcessFactory.distance,
                BufferStatisticsProcessFactory.distance.sample);
        String countField = (String) Params.getValue(input,
                BufferStatisticsProcessFactory.countField,
                BufferStatisticsProcessFactory.countField.sample);
        String statisticsFields = (String) Params.getValue(input,
                BufferStatisticsProcessFactory.statisticsFields,
                BufferStatisticsProcessFactory.statisticsFields.sample);
        if (inputFeatures == null || pointFeatures == null) {
            throw new NullPointerException("inputFeatures and pointFeatures parameters required");
        }

        if (distance == 0) {
            Class<?> geomBinding = inputFeatures.getSchema().getGeometryDescriptor().getType()
                    .getBinding();
            if (!geomBinding.isAssignableFrom(Polygon.class)
                    && !geomBinding.isAssignableFrom(MultiPolygon.class)) {
                throw new NullPointerException("distance parameter required");
            }
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            PointStatisticsOperation operation = new PointStatisticsOperation();
            operation.setBufferDistance(distance);
            resultFc = operation
                    .execute(inputFeatures, countField, statisticsFields, pointFeatures);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(BufferStatisticsProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
