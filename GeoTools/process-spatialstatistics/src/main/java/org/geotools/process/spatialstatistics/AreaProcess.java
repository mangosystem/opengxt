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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Calculates area for each feature in a polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AreaProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(AreaProcess.class);

    public AreaProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static Double process(SimpleFeatureCollection inputFeatures, Filter filter,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AreaProcessFactory.inputFeatures.key, inputFeatures);
        map.put(AreaProcessFactory.filter.key, filter);

        Process process = new AreaProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (Double) resultMap.get(AreaProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new Double(0);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                AreaProcessFactory.inputFeatures, null);
        Filter filter = (Filter) Params.getValue(input, AreaProcessFactory.filter, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        double area = 0.0;
        filter = filter == null ? Filter.INCLUDE : filter;
        SimpleFeatureIterator featureIter = inputFeatures.subCollection(filter).features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                area += geometry.getArea();
            }
        } finally {
            featureIter.close();
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(AreaProcessFactory.RESULT.key, new Double(area));
        return resultMap;
    }
}
