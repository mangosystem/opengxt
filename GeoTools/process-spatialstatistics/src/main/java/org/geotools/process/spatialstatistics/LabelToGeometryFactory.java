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

import java.awt.Font;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * FeatureToLineProcessFactory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LabelToGeometryFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(LabelToGeometryFactory.class);

    private static final String PROCESS_NAME = "LabelToGeometry";

    /*
     * FeatureToLine(SimpleFeatureCollection inputFeatures, Boolean preserveAttributes): SimpleFeatureCollection
     */

    public LabelToGeometryFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }

    @Override
    public Process create() {
        return new FeatureToLineProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("LabelToGeometry.title");
    }

    @Override
    public InternationalString getDescription() {
        return getResource("LabelToGeometry.description");
    }

    /** inputFeatures */
    public static final Parameter<SimpleFeatureCollection> inputFeatures = new Parameter<SimpleFeatureCollection>(
            "inputFeatures", SimpleFeatureCollection.class,
            getResource("LabelToGeometry.inputFeatures.title"),
            getResource("LabelToGeometry.inputFeatures.description"), true, 1, 1, null, new KVP(
                    Params.FEATURES, Params.Polyline));

    /** preserveAttributes */
    public static final Parameter<String> labelAttributeName = new Parameter<String>(
            "labelAttributeName", String.class,
            getResource("LabelToGeometry.labelAttributeName.title"),
            getResource("LabelToGeometry.labelAttributeName.description"), false, 0, 1, null,
            null);
    /** font name */
    public static final Parameter<String> fontName = new Parameter<String>(
            "fontName", String.class,
            getResource("LabelToGeometry.fontName.title"),
            getResource("LabelToGeometry.fontName.description"), false, 0, 1, "Arial",
            null);
    /** font type */
    public static final Parameter<Integer> fontType = new Parameter<Integer>(
            "fontType", Integer.class,
            getResource("LabelToGeometry.fontType.title"),
            getResource("LabelToGeometry.fontType.description"), false, 0, 1, Font.PLAIN,
            null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFeatures.key, inputFeatures);
        parameterInfo.put(labelAttributeName.key, labelAttributeName);
        parameterInfo.put(fontName.key, fontName);
        parameterInfo.put(fontType.key, fontType);
        return parameterInfo;
    }

    /** result */
    public static final Parameter<SimpleFeatureCollection> RESULT = new Parameter<SimpleFeatureCollection>(
            "result", SimpleFeatureCollection.class, getResource("FeatureToLine.result.title"),
            getResource("FeatureToLine.result.description"));

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
