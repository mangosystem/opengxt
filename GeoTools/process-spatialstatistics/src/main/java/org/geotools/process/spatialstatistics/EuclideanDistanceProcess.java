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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterEuclideanDistanceOperation;
import org.geotools.util.logging.Logging;

/**
 * Calculates, for each cell, the Euclidean distance to the closest source.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class EuclideanDistanceProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(EuclideanDistanceProcess.class);

    public EuclideanDistanceProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures,
            KernelType kernelType, Double maximumDistance, Double cellSize,
            ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(EuclideanDistanceProcessFactory.inputFeatures.key, inputFeatures);
        map.put(EuclideanDistanceProcessFactory.maximumDistance.key, maximumDistance);
        map.put(EuclideanDistanceProcessFactory.cellSize.key, cellSize);
        map.put(EuclideanDistanceProcessFactory.extent.key, extent);

        Process process = new EuclideanDistanceProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(EuclideanDistanceProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                EuclideanDistanceProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        Double maximumDistance = (Double) Params.getValue(input,
                EuclideanDistanceProcessFactory.maximumDistance,
                EuclideanDistanceProcessFactory.maximumDistance.sample);
        Double cellSize = (Double) Params.getValue(input, EuclideanDistanceProcessFactory.cellSize,
                EuclideanDistanceProcessFactory.cellSize.sample);
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                EuclideanDistanceProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = extent == null ? inputFeatures.getBounds() : extent;

        // get default cell size from extent
        if (cellSize == null || Double.isNaN(cellSize) || cellSize == 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        if (maximumDistance == null || maximumDistance <= 0 || maximumDistance.isNaN()) {
            maximumDistance = Double.MAX_VALUE;
        }

        GridCoverage2D resultGc = null;
        RasterEuclideanDistanceOperation process = new RasterEuclideanDistanceOperation();
        process.setExtentAndCellSize(boundingBox, cellSize, cellSize);
        resultGc = process.execute(inputFeatures, maximumDistance);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(EuclideanDistanceProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
