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
import org.geotools.process.spatialstatistics.enumeration.RasterPixelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterInterpolationTINOperation;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Interpolates a raster surface from points using an Triangulated Irregular Network(TIN) technique.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TINInterpolationProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(TINInterpolationProcess.class);

    public TINInterpolationProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures,
            Expression inputField, RasterPixelType pixelType, Double cellSize,
            ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TINInterpolationProcessFactory.inputFeatures.key, inputFeatures);
        map.put(TINInterpolationProcessFactory.inputField.key, inputField);
        map.put(TINInterpolationProcessFactory.pixelType.key, pixelType);

        map.put(TINInterpolationProcessFactory.cellSize.key, cellSize);
        map.put(TINInterpolationProcessFactory.extent.key, extent);

        Process process = new TINInterpolationProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(TINInterpolationProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                TINInterpolationProcessFactory.inputFeatures, null);
        Expression inputField = (Expression) Params.getValue(input,
                TINInterpolationProcessFactory.inputField, null);
        if (inputFeatures == null || inputField == null) {
            throw new NullPointerException("inputFeatures, inputField parameters required");
        }

        RasterPixelType pixelType = (RasterPixelType) Params.getValue(input,
                TINInterpolationProcessFactory.pixelType,
                TINInterpolationProcessFactory.pixelType.sample);

        Double cellSize = (Double) Params.getValue(input, TINInterpolationProcessFactory.cellSize,
                TINInterpolationProcessFactory.cellSize.sample);
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                TINInterpolationProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = extent == null ? inputFeatures.getBounds() : extent;

        // get default cell size from extent
        if (cellSize == 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        GridCoverage2D resultGc = null;
        RasterInterpolationTINOperation process = new RasterInterpolationTINOperation();
        process.setExtentAndCellSize(boundingBox, cellSize, cellSize);
        resultGc = process.execute(inputFeatures, inputField, pixelType);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(TINInterpolationProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
