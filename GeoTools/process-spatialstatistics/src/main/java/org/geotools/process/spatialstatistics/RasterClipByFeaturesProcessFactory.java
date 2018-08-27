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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * RasterClipByFeaturesProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterClipByFeaturesProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging
            .getLogger(RasterClipByFeaturesProcessFactory.class);

    private static final String PROCESS_NAME = "RasterClipByFeatures";

    /*
     * RasterClipByFeatures(GridCoverage2D inputCoverage, SimpleFeatureCollection cropFeatures): GridCoverage2D
     */

    public RasterClipByFeaturesProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new RasterClipByFeaturesProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("RasterClipByFeatures.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("RasterClipByFeatures.description");
    }

    /** inputCoverage */
    public static final Parameter<GridCoverage2D> inputCoverage = new Parameter<GridCoverage2D>(
            "inputCoverage", GridCoverage2D.class,
            getResource("RasterClipByFeatures.inputCoverage.title"),
            getResource("RasterClipByFeatures.inputCoverage.description"), true, 1, 1, null, null);

    /** cropFeatures */
    public static final Parameter<SimpleFeatureCollection> cropFeatures = new Parameter<SimpleFeatureCollection>(
            "cropFeatures", SimpleFeatureCollection.class,
            getResource("RasterClipByFeatures.cropFeatures.title"),
            getResource("RasterClipByFeatures.cropFeatures.description"), true, 1, 1, null,
            new KVP(Params.FEATURES, Params.Polygon));

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputCoverage.key, inputCoverage);
        parameterInfo.put(cropFeatures.key, cropFeatures);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>("result",
            GridCoverage2D.class, getResource("RasterClipByFeatures.result.title"),
            getResource("RasterClipByFeatures.result.description"));

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(RESULT.key, RESULT);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

}
