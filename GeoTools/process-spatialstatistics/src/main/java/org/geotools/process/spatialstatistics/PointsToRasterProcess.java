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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.PointAssignmentType;
import org.geotools.process.spatialstatistics.gridcoverage.PointsToRasterOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Converts point features to a raster dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsToRasterProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PointsToRasterProcess.class);

    public PointsToRasterProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures, String inputField,
            PointAssignmentType cellAssignment, Double cellSize, ReferencedEnvelope extent,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PointsToRasterProcessFactory.inputFeatures.key, inputFeatures);
        map.put(PointsToRasterProcessFactory.inputField.key, inputField);
        map.put(PointsToRasterProcessFactory.cellAssignment.key, cellAssignment);
        map.put(PointsToRasterProcessFactory.cellSize.key, cellSize);
        map.put(PointsToRasterProcessFactory.extent.key, extent);

        Process process = new PointsToRasterProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(PointsToRasterProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                PointsToRasterProcessFactory.inputFeatures, null);
        String inputField = (String) Params.getValue(input,
                PointsToRasterProcessFactory.inputField, null);
        if (inputFeatures == null || inputField == null || inputField.trim().length() == 0) {
            throw new NullPointerException("inputFeatures, inputField parameters required");
        }

        PointAssignmentType cellAssignment = (PointAssignmentType) Params.getValue(input,
                PointsToRasterProcessFactory.cellAssignment,
                PointsToRasterProcessFactory.cellAssignment.sample);

        Double cellSize = (Double) Params.getValue(input, PointsToRasterProcessFactory.cellSize,
                PointsToRasterProcessFactory.cellSize.sample);

        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                PointsToRasterProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = extent == null ? inputFeatures.getBounds() : extent;

        // get default cell size from extent
        if (cellSize <= 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        PointsToRasterOperation process = new PointsToRasterOperation();
        process.setExtentAndCellSize(boundingBox, cellSize, cellSize);
        GridCoverage2D resultGc = process.execute(inputFeatures, inputField, cellAssignment);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PointsToRasterProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
