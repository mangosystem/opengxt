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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.autocorrelation.AbstractStatisticsOperation;
import org.geotools.process.spatialstatistics.autocorrelation.LocalGStatisticOperation;
import org.geotools.process.spatialstatistics.autocorrelation.LocalGearysCOperation;
import org.geotools.process.spatialstatistics.autocorrelation.LocalLeesSOperation;
import org.geotools.process.spatialstatistics.autocorrelation.LocalMoranIStatisticOperation;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.AutoCorrelationMethod;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.operations.PointsInPolygonOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Performs local spatial autocorrelation with polygon & point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalSAOverlayProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalSAOverlayProcess.class);

    public LocalSAOverlayProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection polygonFeatures,
            SimpleFeatureCollection pointFeatures, Expression weight,
            AutoCorrelationMethod saMethod, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalSAOverlayProcessFactory.polygonFeatures.key, polygonFeatures);
        map.put(LocalSAOverlayProcessFactory.pointFeatures.key, pointFeatures);
        map.put(LocalSAOverlayProcessFactory.weight.key, weight);
        map.put(LocalSAOverlayProcessFactory.saMethod.key, saMethod);
        map.put(LocalSAOverlayProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalSAOverlayProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalSAOverlayProcessFactory.standardization.key, standardization);
        map.put(LocalSAOverlayProcessFactory.searchDistance.key, searchDistance);

        Process process = new LocalSAOverlayProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(LocalSAOverlayProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(input,
                LocalSAOverlayProcessFactory.polygonFeatures, null);
        SimpleFeatureCollection pointFeatures = (SimpleFeatureCollection) Params.getValue(input,
                LocalSAOverlayProcessFactory.pointFeatures, null);
        if (polygonFeatures == null || polygonFeatures == null) {
            throw new NullPointerException("polygonFeatures, pointFeatures parameters required");
        }

        Expression weight = (Expression) Params.getValue(input, LocalSAOverlayProcessFactory.weight,
                null);

        AutoCorrelationMethod saMethod = (AutoCorrelationMethod) Params.getValue(input,
                LocalSAOverlayProcessFactory.saMethod,
                LocalSAOverlayProcessFactory.saMethod.sample);

        SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                LocalSAOverlayProcessFactory.spatialConcept,
                LocalSAOverlayProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                LocalSAOverlayProcessFactory.distanceMethod,
                LocalSAOverlayProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                LocalSAOverlayProcessFactory.standardization,
                LocalSAOverlayProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                LocalSAOverlayProcessFactory.searchDistance,
                LocalSAOverlayProcessFactory.searchDistance.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            PointsInPolygonOperation pipOperation = new PointsInPolygonOperation();
            SimpleFeatureCollection overlayFc = pipOperation.execute(polygonFeatures, pointFeatures,
                    weight);

            resultFc = getLocalSpatialAutcorrelation(overlayFc, PointsInPolygonOperation.AGG_FIELD,
                    saMethod, spatialConcept, distanceMethod, standardization, searchDistance);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(LocalSAOverlayProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

    private SimpleFeatureCollection getLocalSpatialAutcorrelation(SimpleFeatureCollection features,
            String inputField, AutoCorrelationMethod saMethod, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance) throws IOException {
        SimpleFeatureCollection resultFc = null;

        AbstractStatisticsOperation process = new LocalMoranIStatisticOperation();
        switch (saMethod) {
        case GearyC:
            process = new LocalGearysCOperation();
            break;
        case GetisOrdGiStar:
            process = new LocalGStatisticOperation();
            break;
        case LeeS:
            process = new LocalLeesSOperation();
            break;
        case MoranI:
            process = new LocalMoranIStatisticOperation();
            break;
        }

        process.setSpatialConceptType(spatialConcept);
        process.setDistanceType(distanceMethod);
        process.setStandardizationType(standardization);

        // searchDistance
        if (searchDistance > 0 && !Double.isNaN(searchDistance)) {
            process.setDistanceBand(searchDistance);
        }

        switch (saMethod) {
        case GearyC:
            resultFc = ((LocalGearysCOperation) process).execute(features, inputField);
            break;
        case GetisOrdGiStar:
            resultFc = ((LocalGStatisticOperation) process).execute(features, inputField);
            break;
        case LeeS:
            resultFc = ((LocalLeesSOperation) process).execute(features, inputField);
            break;
        case MoranI:
            resultFc = ((LocalMoranIStatisticOperation) process).execute(features, inputField);
            break;
        }

        return resultFc;
    }
}