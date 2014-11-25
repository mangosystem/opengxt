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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.FishnetType;
import org.geotools.process.spatialstatistics.operations.FishnetOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Creates a fishnet of rectangular cell polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FishnetProcess.class);

    private boolean started = false;

    public FishnetProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Boolean boundaryInside, Integer columns,
            Integer rows, Double width, Double height, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FishnetProcessFactory.extent.key, extent);
        map.put(FishnetProcessFactory.boundsSource.key, boundsSource);
        map.put(FishnetProcessFactory.boundaryInside.key, boundaryInside);
        map.put(FishnetProcessFactory.columns.key, columns);
        map.put(FishnetProcessFactory.rows.key, rows);
        map.put(FishnetProcessFactory.width.key, width);
        map.put(FishnetProcessFactory.height.key, height);

        Process process = new FishnetProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(FishnetProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    FishnetProcessFactory.extent, null);
            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    FishnetProcessFactory.boundsSource, null);
            if (extent == null && boundsSource == null) {
                throw new NullPointerException("extent or boundsSource parameters required");
            }

            Boolean boundaryInside = (Boolean) Params.getValue(input,
                    FishnetProcessFactory.boundaryInside,
                    FishnetProcessFactory.boundaryInside.sample);
            Integer columns = (Integer) Params.getValue(input, FishnetProcessFactory.columns,
                    FishnetProcessFactory.columns.sample);
            Integer rows = (Integer) Params.getValue(input, FishnetProcessFactory.rows,
                    FishnetProcessFactory.rows.sample);
            Double width = (Double) Params.getValue(input, FishnetProcessFactory.width,
                    FishnetProcessFactory.width.sample);
            Double height = (Double) Params.getValue(input, FishnetProcessFactory.height,
                    FishnetProcessFactory.height.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            SimpleFeatureCollection resultFc = null;
            FishnetOperation operation = new FishnetOperation();
            operation.setBoundaryInside(boundaryInside);
            operation.setFishnetType(FishnetType.Rectangle);
            if (boundsSource != null) {
                try {
                    Geometry geometryBoundary = unionPolygonFeatures(boundsSource);
                    operation.setGeometryBoundary(geometryBoundary);
                } catch (Exception e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }

            if (extent == null && boundsSource != null) {
                extent = boundsSource.getBounds();
            }
            
            if (columns == 0 || rows == 0) {
                resultFc = operation.execute(extent, width, height);
            } else {
                resultFc = operation.execute(extent, columns, rows);
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(FishnetProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    private Geometry unionPolygonFeatures(SimpleFeatureCollection features) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                geometries.add(geometry);
            }
        } finally {
            featureIter.close();
        }

        CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
        return unionOp.union();
    }
}
