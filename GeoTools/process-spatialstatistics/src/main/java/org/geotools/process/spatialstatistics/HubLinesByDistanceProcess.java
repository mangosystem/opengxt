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
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.operations.HubLinesByDistanceOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a line features representing the shortest distance between hub and spoke features by nearest distance.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HubLinesByDistanceProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(HubLinesByDistanceProcess.class);

    public HubLinesByDistanceProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection hubFeatures,
            String hubIdField, SimpleFeatureCollection spokeFeatures, boolean preserveAttributes,
            boolean useCentroid, boolean useBezierCurve, double maximumDistance,
            ProgressListener monitor) {
        return process(hubFeatures, hubIdField, spokeFeatures, preserveAttributes, useCentroid,
                useBezierCurve, maximumDistance, DistanceUnit.Default, monitor);
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection hubFeatures,
            String hubIdField, SimpleFeatureCollection spokeFeatures, boolean preserveAttributes,
            boolean useCentroid, boolean useBezierCurve, double maximumDistance,
            DistanceUnit distanceUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HubLinesByDistanceProcessFactory.hubFeatures.key, hubFeatures);
        map.put(HubLinesByDistanceProcessFactory.hubIdField.key, hubIdField);
        map.put(HubLinesByDistanceProcessFactory.spokeFeatures.key, spokeFeatures);
        map.put(HubLinesByDistanceProcessFactory.preserveAttributes.key, preserveAttributes);
        map.put(HubLinesByDistanceProcessFactory.useCentroid.key, useCentroid);
        map.put(HubLinesByDistanceProcessFactory.useBezierCurve.key, useBezierCurve);
        map.put(HubLinesByDistanceProcessFactory.maximumDistance.key, maximumDistance);
        map.put(HubLinesByDistanceProcessFactory.distanceUnit.key, distanceUnit);

        Process process = new HubLinesByDistanceProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(HubLinesByDistanceProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection hubFeatures = (SimpleFeatureCollection) Params.getValue(input,
                HubLinesByDistanceProcessFactory.hubFeatures, null);
        String hubIdField = (String) Params.getValue(input,
                HubLinesByDistanceProcessFactory.hubIdField, null);
        SimpleFeatureCollection spokeFeatures = (SimpleFeatureCollection) Params.getValue(input,
                HubLinesByDistanceProcessFactory.spokeFeatures, null);
        if (hubFeatures == null || spokeFeatures == null) {
            throw new NullPointerException("hubFeatures, spokeFeatures parameters required");
        }

        Boolean preserveAttributes = (Boolean) Params.getValue(input,
                HubLinesByDistanceProcessFactory.preserveAttributes,
                HubLinesByDistanceProcessFactory.preserveAttributes.sample);
        Boolean useCentroid = (Boolean) Params.getValue(input,
                HubLinesByDistanceProcessFactory.useCentroid,
                HubLinesByDistanceProcessFactory.useCentroid.sample);
        Boolean useBezierCurve = (Boolean) Params.getValue(input,
                HubLinesByDistanceProcessFactory.useBezierCurve,
                HubLinesByDistanceProcessFactory.useBezierCurve.sample);
        Double maximumDistance = (Double) Params.getValue(input,
                HubLinesByDistanceProcessFactory.maximumDistance,
                HubLinesByDistanceProcessFactory.maximumDistance.sample);
        DistanceUnit distanceUnit = (DistanceUnit) Params.getValue(input,
                HubLinesByDistanceProcessFactory.distanceUnit,
                HubLinesByDistanceProcessFactory.distanceUnit.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            HubLinesByDistanceOperation operation = new HubLinesByDistanceOperation();
            operation.setUseBezierCurve(useBezierCurve);
            resultFc = operation.execute(hubFeatures, hubIdField, spokeFeatures, useCentroid,
                    preserveAttributes, maximumDistance, distanceUnit);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(HubLinesByDistanceProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
