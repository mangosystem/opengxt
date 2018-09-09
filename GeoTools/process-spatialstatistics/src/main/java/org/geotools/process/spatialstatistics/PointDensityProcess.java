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
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterNeighborhood;
import org.geotools.process.spatialstatistics.gridcoverage.RasterNeighborhood.NeighborUnits;
import org.geotools.process.spatialstatistics.gridcoverage.RasterPointDensityOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates a magnitude per unit area from point features that fall within a neighborhood around each cell.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointDensityProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PointDensityProcess.class);

    public PointDensityProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures,
            KernelType kernelType, String populationField, String neighborhood, Double cellSize,
            ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PointDensityProcessFactory.inputFeatures.key, inputFeatures);
        map.put(PointDensityProcessFactory.populationField.key, populationField);
        map.put(PointDensityProcessFactory.neighborhood.key, neighborhood);
        map.put(PointDensityProcessFactory.cellSize.key, cellSize);
        map.put(PointDensityProcessFactory.extent.key, extent);

        Process process = new PointDensityProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(PointDensityProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                PointDensityProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        String populationField = (String) Params.getValue(input,
                PointDensityProcessFactory.populationField, null);
        String neighborhood = (String) Params.getValue(input,
                PointDensityProcessFactory.neighborhood,
                PointDensityProcessFactory.neighborhood.sample);
        Double cellSize = (Double) Params.getValue(input, PointDensityProcessFactory.cellSize,
                PointDensityProcessFactory.cellSize.sample);
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                PointDensityProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = extent == null ? inputFeatures.getBounds() : extent;

        // get default cell size from extent
        if (cellSize == null || Double.isNaN(cellSize) || cellSize == 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        // Neighborhood: Circle + Radius, Rectangle + width + height
        double searchRadius = 0;
        RasterNeighborhood rnh = new RasterNeighborhood();
        if (neighborhood != null && neighborhood.trim().length() > 0) {
            neighborhood = neighborhood.toUpperCase();
            String[] splits = neighborhood.split("\\+");

            if (neighborhood.contains("CIRCLE")) {
                searchRadius = splits.length >= 2 ? Double.parseDouble(splits[1]) : 0;
                rnh.setCircle(searchRadius, NeighborUnits.MAP);
            } else if (splits.length >= 3) {
                double width = Double.parseDouble(splits[1]);
                double height = Double.parseDouble(splits[2]);
                rnh.setRectangle(width, height, NeighborUnits.MAP);
                searchRadius = width;
            }
        } else {
            // Default = Circle + Radius(divided by 30)
            searchRadius = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 30.0;
            rnh.setCircle(searchRadius, NeighborUnits.MAP);
            LOGGER.warning("default neighborhood = Circle + Radius(" + searchRadius + ")");
        }

        GridCoverage2D resultGc = null;
        RasterPointDensityOperation process = new RasterPointDensityOperation();
        process.getRasterEnvironment().setExtent(boundingBox);
        process.getRasterEnvironment().setCellSizeX(cellSize);
        process.getRasterEnvironment().setCellSizeY(cellSize);
        process.setNeighbor(rnh);

        resultGc = process.execute(inputFeatures, populationField);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PointDensityProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
