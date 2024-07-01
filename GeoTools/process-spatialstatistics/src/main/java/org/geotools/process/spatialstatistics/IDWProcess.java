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

import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterInterpolationIDWOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterInterpolationOperator.RadiusType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterRadius;
import org.geotools.util.logging.Logging;

/**
 * Interpolates a surface from points using an inverse distance weighted (IDW) technique.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class IDWProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(IDWProcess.class);

    public IDWProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures, String inputField,
            Double power, RadiusType radiusType, Integer numberOfPoints, Double distance,
            Double cellSize, ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(IDWProcessFactory.inputFeatures.key, inputFeatures);
        map.put(IDWProcessFactory.inputField.key, inputField);
        map.put(IDWProcessFactory.power.key, power);
        map.put(IDWProcessFactory.radiusType.key, radiusType);
        map.put(IDWProcessFactory.numberOfPoints.key, numberOfPoints);
        map.put(IDWProcessFactory.distance.key, distance);

        map.put(IDWProcessFactory.cellSize.key, cellSize);
        map.put(IDWProcessFactory.extent.key, extent);

        Process process = new IDWProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(IDWProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                IDWProcessFactory.inputFeatures, null);
        String inputField = (String) Params.getValue(input, IDWProcessFactory.inputField, null);
        if (inputFeatures == null || inputField == null || inputField.trim().length() == 0) {
            throw new NullPointerException("inputFeatures, inputField parameters required");
        }

        inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
        if (inputFeatures.getSchema().indexOf(inputField) == -1) {
            throw new NullPointerException(inputField + " does not exist!");
        }

        Double power = (Double) Params.getValue(input, IDWProcessFactory.power,
                IDWProcessFactory.power.sample);
        RadiusType radiusType = (RadiusType) Params.getValue(input, IDWProcessFactory.radiusType,
                IDWProcessFactory.radiusType.sample);
        Integer numberOfPoints = (Integer) Params.getValue(input, IDWProcessFactory.numberOfPoints,
                IDWProcessFactory.numberOfPoints.sample);
        Double distance = (Double) Params.getValue(input, IDWProcessFactory.distance,
                IDWProcessFactory.distance.sample);

        Double cellSize = (Double) Params.getValue(input, IDWProcessFactory.cellSize,
                IDWProcessFactory.cellSize.sample);
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                IDWProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = inputFeatures.getBounds();
        if (extent != null) {
            boundingBox = extent;
        }

        // get default cell size from extent
        if (cellSize == 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        power = power == 0 ? 2.0 : power;

        RasterRadius rasterRadius = new RasterRadius();
        if (radiusType == RadiusType.Variable) {
            numberOfPoints = numberOfPoints == 0 ? 12 : numberOfPoints;
            if (distance > 0)
                rasterRadius.setVariable(numberOfPoints, distance);
            else
                rasterRadius.setVariable(numberOfPoints);
        } else {
            // The default radius is five times the cell size of the output raster.
            distance = distance == 0 ? cellSize * 5 : distance;
            if (numberOfPoints > 0)
                rasterRadius.setFixed(distance, numberOfPoints);
            else
                rasterRadius.setFixed(distance);
        }

        GridCoverage2D resultGc = null;
        RasterInterpolationIDWOperation process = new RasterInterpolationIDWOperation();
        process.setExtentAndCellSize(boundingBox, cellSize, cellSize);
        resultGc = process.execute(inputFeatures, inputField, power, rasterRadius);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(IDWProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
