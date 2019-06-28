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
import org.geotools.process.spatialstatistics.operations.PointsToLineOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates line features from points.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsToLineProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PointsToLineProcess.class);

    public PointsToLineProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String lineField, String sortField, Boolean useBezierCurve, Boolean closeLine,
            ProgressListener monitor) {
        return process(inputFeatures, lineField, sortField, useBezierCurve, closeLine,
                Boolean.FALSE, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String lineField, String sortField, Boolean useBezierCurve, Boolean closeLine,
            Boolean geodesicLine, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PointsToLineProcessFactory.inputFeatures.key, inputFeatures);
        map.put(PointsToLineProcessFactory.lineField.key, lineField);
        map.put(PointsToLineProcessFactory.sortField.key, sortField);
        map.put(PointsToLineProcessFactory.useBezierCurve.key, useBezierCurve);
        map.put(PointsToLineProcessFactory.closeLine.key, closeLine);
        map.put(PointsToLineProcessFactory.geodesicLine.key, geodesicLine);

        Process process = new PointsToLineProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(PointsToLineProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                PointsToLineProcessFactory.inputFeatures, null);
        String lineField = (String) Params.getValue(input, PointsToLineProcessFactory.lineField,
                null);
        String sortField = (String) Params.getValue(input, PointsToLineProcessFactory.sortField,
                null);
        Boolean useBezierCurve = (Boolean) Params.getValue(input,
                PointsToLineProcessFactory.useBezierCurve,
                PointsToLineProcessFactory.useBezierCurve.sample);
        Boolean closeLine = (Boolean) Params.getValue(input, PointsToLineProcessFactory.closeLine,
                PointsToLineProcessFactory.closeLine.sample);
        Boolean geodesicLine = (Boolean) Params.getValue(input,
                PointsToLineProcessFactory.geodesicLine,
                PointsToLineProcessFactory.geodesicLine.sample);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            PointsToLineOperation operation = new PointsToLineOperation();
            operation.setUseBezierCurve(useBezierCurve);
            operation.setCloseLine(closeLine);
            operation.setGeodesicLine(geodesicLine);
            resultFc = operation.execute(inputFeatures, lineField, sortField, closeLine);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PointsToLineProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

}
