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

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.ClipWithGeometryFeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * Extracts input features that overlay the clip geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ClipWithGeometryProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ClipWithGeometryProcess.class);

    final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public ClipWithGeometryProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Geometry clipGeometry, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ClipWithGeometryProcessFactory.inputFeatures.key, inputFeatures);
        map.put(ClipWithGeometryProcessFactory.clipGeometry.key, clipGeometry);

        Process process = new ClipWithGeometryProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(ClipWithGeometryProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ClipWithGeometryProcessFactory.inputFeatures, null);
        Geometry clipGeometry = (Geometry) Params.getValue(input,
                ClipWithGeometryProcessFactory.clipGeometry, null);
        if (inputFeatures == null || clipGeometry == null) {
            throw new NullPointerException("All parameter required");
        }

        // start process
        // apply bbox filter
        String geomName = inputFeatures.getSchema().getGeometryDescriptor().getLocalName();

        CoordinateReferenceSystem crs = inputFeatures.getSchema().getCoordinateReferenceSystem();
        clipGeometry = transformGeometry(clipGeometry, crs);

        Filter filter = ff.bbox(ff.property(geomName),
                new ReferencedEnvelope(clipGeometry.getEnvelopeInternal(), crs));

        // clip
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new ClipWithGeometryFeatureCollection(inputFeatures.subCollection(filter),
                        clipGeometry));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ClipWithGeometryProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
