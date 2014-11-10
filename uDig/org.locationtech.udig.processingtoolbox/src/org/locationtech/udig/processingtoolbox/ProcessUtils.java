/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Process Utilities class
 * 
 * @author MapPlus
 * 
 */
@SuppressWarnings("nls")
public class ProcessUtils {

    public enum OutputType {
        FEATURES, RASTER, GEOMETRY, ENVELOPE, OTHER
    }

    public static String getWindowTitle(String processName) {
        String windowTitle = Character.toUpperCase(processName.charAt(0)) + processName.substring(1);
        if (!processName.contains("ST_")) { 
            if (windowTitle.substring(2, 3).equalsIgnoreCase("_")) {
                windowTitle = windowTitle.substring(3);
            }

            StringBuffer sb = new StringBuffer();
            for (int index = 0; index < windowTitle.length(); index++) {
                char cat = windowTitle.charAt(index);
                if (index > 0 && Character.isUpperCase(cat)) {
                    sb.append(" ").append(cat);
                } else {
                    sb.append(cat);
                }
            }
            return sb.toString();
        } else {
            return windowTitle;
        }
    }

    public static String getLayerName(Name processName) {
        String layerName = processName.getLocalPart().toLowerCase();
        if (layerName.substring(2, 3).equalsIgnoreCase("_")) {
            layerName = layerName.substring(3);
        }

        return "udig_" + layerName;
    }

    public static boolean postprocessRequired(ProcessFactory factory, Name processName) {
        Map<String, Parameter<?>> paramInfo = factory.getResultInfo(processName, null);
        boolean requireProgress = false;
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            Class<?> binding = entrySet.getValue().type;
            if (binding.isAssignableFrom(SimpleFeatureCollection.class)) {
                requireProgress = true;
            } else if (binding.isAssignableFrom(GridCoverage2D.class)) {
                requireProgress = true;
            } else if (binding.isAssignableFrom(Geometry.class)) {
                requireProgress = true;
            } else if (binding.isAssignableFrom(ReferencedEnvelope.class)) {
                requireProgress = true;
            } else if (binding.isAssignableFrom(BoundingBox.class)) {
                requireProgress = true;
            }
        }
        return requireProgress;
    }

    public static OutputType getOutputType(ProcessFactory factory, Name processName) {
        Map<String, Parameter<?>> paramInfo = factory.getResultInfo(processName, null);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            Class<?> binding = entrySet.getValue().type;
            if (binding.isAssignableFrom(SimpleFeatureCollection.class)) {
                return OutputType.FEATURES;
            } else if (binding.isAssignableFrom(GridCoverage2D.class)) {
                return OutputType.RASTER;
            } else if (binding.isAssignableFrom(Geometry.class)) {
                return OutputType.GEOMETRY;
            } else if (binding.isAssignableFrom(ReferencedEnvelope.class)) {
                return OutputType.ENVELOPE;
            } else if (binding.isAssignableFrom(BoundingBox.class)) {
                return OutputType.ENVELOPE;
            }
        }
        return OutputType.OTHER;
    }

    public static boolean outputLocationRequired(ProcessFactory factory, Name processName) {
        Map<String, Parameter<?>> paramInfo = factory.getResultInfo(processName, null);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            Class<?> binding = entrySet.getValue().type;
            if (binding.isAssignableFrom(SimpleFeatureCollection.class)) {
                return true;
            } else if (binding.isAssignableFrom(GridCoverage2D.class)) {
                return true;
            } else if (binding.isAssignableFrom(Geometry.class)) {
                return true;
            } else if (binding.isAssignableFrom(ReferencedEnvelope.class)) {
                return true;
            } else if (binding.isAssignableFrom(BoundingBox.class)) {
                return true;
            }
        }
        return false;
    }

    public static String getFeatureInformation(SimpleFeatureCollection sfc) {
        final String separator = System.getProperty("line.separator");
        final String seperator = " | ";
        
        StringBuffer sb = new StringBuffer();
        SimpleFeatureType schema = sfc.getSchema();
        List<AttributeDescriptor> attDescs = schema.getAttributeDescriptors();
        for (int index = 0; index < attDescs.size(); index++) {
            AttributeDescriptor descriptor = attDescs.get(index);
            sb.append(descriptor.getLocalName());
            if (index < attDescs.size() - 1) {
                sb.append(seperator);
            }
        }
        sb.append(separator);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = sfc.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                for (int index = 0; index < attDescs.size(); index++) {
                    sb.append(feature.getAttribute(index));
                    if (index < attDescs.size() - 1) {
                        sb.append(seperator);
                    }
                }
                sb.append(separator);
            }
        } finally {
            featureIter.close();
        }

        return sb.toString();
    }
}
