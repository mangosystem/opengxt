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
import org.geotools.geometry.jts.ReferencedEnvelope;
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
import org.geotools.process.spatialstatistics.enumeration.BinningGridType;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.pattern.CircularBinningOperation;
import org.geotools.process.spatialstatistics.pattern.HexagonalBinningOperation;
import org.geotools.process.spatialstatistics.pattern.RectangularBinningOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Performs local spatial autocorrelation with point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalSABinningProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LocalSABinningProcess.class);

    public LocalSABinningProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection features,
            Expression weight, BinningGridType gridType, ReferencedEnvelope extent, Double size,
            AutoCorrelationMethod saMethod, SpatialConcept spatialConcept,
            DistanceMethod distanceMethod, StandardizationMethod standardization,
            Double searchDistance, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LocalSABinningProcessFactory.features.key, features);
        map.put(LocalSABinningProcessFactory.weight.key, weight);
        map.put(LocalSABinningProcessFactory.gridType.key, gridType);
        map.put(LocalSABinningProcessFactory.extent.key, extent);
        map.put(LocalSABinningProcessFactory.size.key, size);
        map.put(LocalSABinningProcessFactory.saMethod.key, saMethod);
        map.put(LocalSABinningProcessFactory.spatialConcept.key, spatialConcept);
        map.put(LocalSABinningProcessFactory.distanceMethod.key, distanceMethod);
        map.put(LocalSABinningProcessFactory.standardization.key, standardization);
        map.put(LocalSABinningProcessFactory.searchDistance.key, searchDistance);

        Process process = new LocalSABinningProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(LocalSABinningProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                LocalSABinningProcessFactory.features, null);
        if (features == null) {
            throw new NullPointerException("features parameter required");
        }

        Expression weight = (Expression) Params.getValue(input, LocalSABinningProcessFactory.weight,
                null);

        BinningGridType gridType = (BinningGridType) Params.getValue(input,
                LocalSABinningProcessFactory.gridType,
                LocalSABinningProcessFactory.gridType.sample);

        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                LocalSABinningProcessFactory.extent, null);

        Double size = (Double) Params.getValue(input, LocalSABinningProcessFactory.size, null);

        AutoCorrelationMethod saMethod = (AutoCorrelationMethod) Params.getValue(input,
                LocalSABinningProcessFactory.saMethod,
                LocalSABinningProcessFactory.saMethod.sample);

        SpatialConcept spatialConcept = (SpatialConcept) Params.getValue(input,
                LocalSABinningProcessFactory.spatialConcept,
                LocalSABinningProcessFactory.spatialConcept.sample);

        DistanceMethod distanceMethod = (DistanceMethod) Params.getValue(input,
                LocalSABinningProcessFactory.distanceMethod,
                LocalSABinningProcessFactory.distanceMethod.sample);

        StandardizationMethod standardization = (StandardizationMethod) Params.getValue(input,
                LocalSABinningProcessFactory.standardization,
                LocalSABinningProcessFactory.standardization.sample);

        Double searchDistance = (Double) Params.getValue(input,
                LocalSABinningProcessFactory.searchDistance,
                LocalSABinningProcessFactory.searchDistance.sample);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            SimpleFeatureCollection binningFc = aggregatePoints(features, weight, gridType, extent,
                    size);

            if (binningFc != null) {
                // val = BinningOperation.AGG_FIELD
                resultFc = getLocalSpatialAutcorrelation(binningFc, "val", saMethod, spatialConcept,
                        distanceMethod, standardization, searchDistance);
            }
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(LocalSABinningProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }

    private SimpleFeatureCollection aggregatePoints(SimpleFeatureCollection features,
            Expression weight, BinningGridType gridType, ReferencedEnvelope extent, Double size)
            throws IOException {
        SimpleFeatureCollection resultFc = null;

        if (extent == null || extent.isEmpty()) {
            extent = features.getBounds();
        }

        if (size == null || size <= 0) {
            size = Math.min(extent.getWidth(), extent.getHeight()) / 40.0d;
            if (gridType == BinningGridType.Circle) {
                size = size / 2.0;
            }
            LOGGER.log(Level.WARNING, "The default width / height is " + size);
        }

        switch (gridType) {
        case Circle:
            CircularBinningOperation circularProcess = new CircularBinningOperation();
            circularProcess.setOnlyValidGrid(false);
            resultFc = circularProcess.execute(features, weight, extent, size);
            break;
        case Hexagon:
            HexagonalBinningOperation hexagonProcess = new HexagonalBinningOperation();
            hexagonProcess.setOnlyValidGrid(false);
            resultFc = hexagonProcess.execute(features, weight, extent, size);
            break;
        case Rectangle:
            RectangularBinningOperation rectProcess = new RectangularBinningOperation();
            rectProcess.setOnlyValidGrid(false);
            resultFc = rectProcess.execute(features, weight, extent, size, size);
            break;
        }

        return resultFc;
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