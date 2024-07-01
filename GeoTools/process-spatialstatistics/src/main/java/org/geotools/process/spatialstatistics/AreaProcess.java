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

import javax.measure.Unit;
import javax.measure.quantity.Area;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.measure.Measure;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import si.uom.SI;

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
        return AreaProcess.process(inputFeatures, filter, AreaUnit.Default, monitor);
    }

    public static Double process(SimpleFeatureCollection inputFeatures, Filter filter,
            AreaUnit areaUnit, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AreaProcessFactory.inputFeatures.key, inputFeatures);
        map.put(AreaProcessFactory.filter.key, filter);
        map.put(AreaProcessFactory.areaUnit.key, areaUnit);

        Process process = new AreaProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (Double) resultMap.get(AreaProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return Double.valueOf(0.0d);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) Params.getValue(input,
                AreaProcessFactory.inputFeatures, null);
        Filter filter = (Filter) Params.getValue(input, AreaProcessFactory.filter, Filter.INCLUDE);
        AreaUnit targetUnit = (AreaUnit) Params.getValue(input, AreaProcessFactory.areaUnit,
                AreaUnit.Default);
        if (features == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        CoordinateReferenceSystem sourceCRS = features.getSchema().getCoordinateReferenceSystem();
        filter = filter == null ? Filter.INCLUDE : filter;
        targetUnit = targetUnit == null ? AreaUnit.Default : targetUnit;

        double dArea = 0d;
        if (sourceCRS == null || targetUnit == AreaUnit.Default) {
            // use AreaUnit.Default
            dArea = sumArea(features.subCollection(filter));
        } else {
            CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(sourceCRS);
            Unit<Area> sourceUnit = SI.SQUARE_METRE; // default
            if (horCRS instanceof GeographicCRS) {
                try {
                    // GeographicCRS to UTM
                    Coordinate p = features.getBounds().centre();
                    CoordinateReferenceSystem autoCRS = CRS.decode("AUTO:42001," + p.x + "," + p.y);
                    dArea = sumArea(new ReprojectFeatureCollection(features.subCollection(filter),
                            autoCRS));
                } catch (NoSuchAuthorityCodeException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                } catch (FactoryException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            } else {
                Unit<?> distUnit = horCRS.getCoordinateSystem().getAxis(0).getUnit();
                sourceUnit = (Unit<Area>) distUnit.multiply(distUnit);
                dArea = sumArea(features.subCollection(filter));
            }

            dArea = UnitConverter.convertArea(new Measure(dArea, sourceUnit), targetUnit);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(AreaProcessFactory.RESULT.key, Double.valueOf(dArea));
        return resultMap;
    }

    private double sumArea(SimpleFeatureCollection inputFeatures) {
        double area = 0.0;
        SimpleFeatureIterator featureIter = inputFeatures.features();
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
        return area;
    }
}
