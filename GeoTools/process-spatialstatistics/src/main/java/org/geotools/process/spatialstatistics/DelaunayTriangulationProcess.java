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
import org.geotools.process.spatialstatistics.operations.DelaunayTrangulationOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates delaunay triangulation polygons from input point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DelaunayTriangulationProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(DelaunayTriangulationProcess.class);

    public DelaunayTriangulationProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Geometry clipArea, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DelaunayTriangulationProcessFactory.inputFeatures.key, inputFeatures);
        map.put(DelaunayTriangulationProcessFactory.clipArea.key, clipArea);

        Process process = new DelaunayTriangulationProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(DelaunayTriangulationProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                DelaunayTriangulationProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Geometry clipArea = (Geometry) Params.getValue(input,
                DelaunayTriangulationProcessFactory.clipArea, null);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            DelaunayTrangulationOperation operation = new DelaunayTrangulationOperation();
            if (clipArea != null) {
                operation.setClipArea(clipArea);
            }
            resultFc = operation.execute(inputFeatures);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(DelaunayTriangulationProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
