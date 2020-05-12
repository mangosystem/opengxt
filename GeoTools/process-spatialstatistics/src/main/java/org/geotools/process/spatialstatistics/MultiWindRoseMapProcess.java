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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.MultiWindRoseOperation;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates a wind roses map from features.
 * 
 * @author jyajya, MangoSystem
 * 
 * @source $URL$
 */
public class MultiWindRoseMapProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(MultiWindRoseMapProcess.class);

    public MultiWindRoseMapProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(Collection<SimpleFeatureCollection> inputFeatures,
            String weightFields, SimpleFeatureCollection centerFeatures, Geometry centerPoint,
            Double searchRadius, String valueField, int roseCnt, ProgressListener monitor) {
        // SimpleFeatureCollection inputFeatures, String weightField,
        // SimpleFeatureCollection centerFeatures, double searchRadius, String valueField, int roseCnt
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(MultiWindRoseMapProcessFactory.inputFeatures.key, inputFeatures);
        map.put(MultiWindRoseMapProcessFactory.weightFields.key, weightFields);
        // map.put(VA_WindRoseFactory.inputFeatures2.key, inputFeatures[1]);
        // map.put(VA_WindRoseFactory.weightField2.key, weightField[1]);
        // map.put(VA_WindRoseFactory.inputFeatures3.key, inputFeatures[2]);
        // map.put(VA_WindRoseFactory.weightField3.key, weightField[2]);
        map.put(MultiWindRoseMapProcessFactory.centerFeatures.key, centerFeatures);
        map.put(MultiWindRoseMapProcessFactory.centerPoint.key, centerPoint);
        map.put(MultiWindRoseMapProcessFactory.searchRadius.key, searchRadius);
        map.put(MultiWindRoseMapProcessFactory.roseCount.key, roseCnt);

        // parameterInfo.put(inputFeatures.key, inputFeatures);
        // parameterInfo.put(weightField.key, weightField);
        // parameterInfo.put(valueField.key, valueField);
        // parameterInfo.put(roseCount.key, roseCount);

        Process process = new MultiWindRoseMapProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(MultiWindRoseMapProcessFactory.result.key);
        } catch (ProcessException e) {
            e.printStackTrace();
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {

        @SuppressWarnings("unchecked")
        Collection<SimpleFeatureCollection> inputFeatures = (Collection<SimpleFeatureCollection>) Params
                .getValue(input, MultiWindRoseMapProcessFactory.inputFeatures, null);

        if (inputFeatures == null || inputFeatures.size() == 0) {
            throw new NullPointerException("input features parameter required");
        }

        String weightFiels = (String) Params.getValue(input,
                MultiWindRoseMapProcessFactory.weightFields, null);

        // SimpleFeatureCollection inputFeatures2 = (SimpleFeatureCollection) ParamUtil.getParam(
        // input, MultiWindRoseMapProcessFactory.inputFeatures2, null);
        //
        // String weightField2 = (String) ParamUtil.getParam(input, VA_WindRoseFactory.weightField2,
        // null);
        // SimpleFeatureCollection inputFeatures3 = (SimpleFeatureCollection) ParamUtil.getParam(
        // input, MultiWindRoseMapProcessFactory.inputFeatures3, null);
        //
        // String weightField3 = (String) ParamUtil.getParam(input, VA_WindRoseFactory.weightField3,
        // null);
        SimpleFeatureCollection centerFeatures = (SimpleFeatureCollection) Params.getValue(input,
                MultiWindRoseMapProcessFactory.centerFeatures, null);
        Geometry centerPoint = (Geometry) Params.getValue(input,
                MultiWindRoseMapProcessFactory.centerPoint, null);
        Double searchRadius = (Double) Params.getValue(input,
                MultiWindRoseMapProcessFactory.searchRadius, null);
        Integer roseCount = (Integer) Params.getValue(input,
                MultiWindRoseMapProcessFactory.roseCount, Integer.valueOf(36));

        // start process
        SimpleFeatureCollection resultFc = null;
        SimpleFeatureCollection anchorFc = null;
        try {
            SimpleFeatureSource sfc = null;
            Collection<SimpleFeatureCollection> tgInputFeatures = inputFeatures;
            String[] tgWeightFields = null;
            try {
                tgWeightFields = weightFiels.split(",");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (weightFiels != null && tgInputFeatures.size() != tgWeightFields.length) {
                throw new ProcessException(
                        "The number of parasmeters InputFeatures and WeightFields in the process are different.");
            }

            if (centerFeatures == null && centerPoint == null) {
                throw new ProcessException(
                        "The number of parasmeters InputFeatures and WeightFields in the process are different.");
            }

            MultiWindRoseOperation process = new MultiWindRoseOperation();
            if (centerFeatures.size() > 0) {
                sfc = process.execute(tgInputFeatures, tgWeightFields, centerFeatures, searchRadius,
                        roseCount);
            } else {
                sfc = process.execute(tgInputFeatures, tgWeightFields, centerPoint, searchRadius,
                        roseCount);
            }

            resultFc = sfc == null ? null : sfc.getFeatures();
            anchorFc = process.getAnchor().getFeatures();
        } catch (Exception ee) {
            ee.printStackTrace();
            monitor.exceptionOccurred(ee);
        }
        // end process

        monitor.setTask(Text.text("Encoding result"));
        monitor.progress(90.0f);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(MultiWindRoseMapProcessFactory.result.key, resultFc);
        resultMap.put(MultiWindRoseMapProcessFactory.anchor.key, anchorFc);
        // resultMap.put(VA_RingMapFactory.ring_anchor.key, anchorFc);
        monitor.complete(); // same as 100.0f

        return resultMap;
    }

}
