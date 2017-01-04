/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.util.logging.Logger;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Combo;
import org.geotools.data.FeatureSource;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.ui.WidgetBuilder;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Abstract AMOEBA Wizard Page
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AmoebaWizardAbstractPage extends WizardPage {
    protected static final Logger LOGGER = Logging.getLogger(AmoebaWizardAbstractPage.class);

    @SuppressWarnings("nls")
    protected static final String FIELD = "abs_4(FIELD)";

    @SuppressWarnings("nls")
    protected final String[] IDX_FIELDS = { "GiZScore", "LLsIndex" };

    @SuppressWarnings("nls")
    protected final String[] algorithm = { "Getis-Ord Gi*", "Lee’s Si*" };

    @SuppressWarnings("nls")
    protected final String[] mathOperators = { " > ", " >= ", " < ", " <= " };

    @SuppressWarnings("nls")
    protected final String[] cIntervals = { "0.84", "1.28", "1.64", "1.96", "2.05", "2.17", "2.33",
            "2.58" };

    @SuppressWarnings("nls")
    protected final String EMPTY = "";

    protected WidgetBuilder wb = WidgetBuilder.newInstance();

    protected org.locationtech.udig.project.internal.Map map;

    protected AmoebaParameter param = new AmoebaParameter();

    protected AmoebaWizardAbstractPage(String pageName) {
        super(pageName);
    }

    protected void fillLayers(IMap map, Combo combo, VectorLayerType layerType) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.hasResource(FeatureSource.class)) {
                GeometryDescriptor descriptor = layer.getSchema().getGeometryDescriptor();
                Class<?> geometryBinding = descriptor.getType().getBinding();
                switch (layerType) {
                case ALL:
                    combo.add(layer.getName());
                    break;
                case LINESTRING:
                    if (geometryBinding.isAssignableFrom(LineString.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POINT:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POLYGON:
                    if (geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case MULTIPART:
                    if (geometryBinding.isAssignableFrom(MultiPolygon.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POLYLINE:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)
                            || geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    protected void fillEnum(Combo combo, Class<?> enumType, int selectedIndex) {
        combo.removeAll();
        for (Object enumVal : enumType.getEnumConstants()) {
            combo.add(enumVal.toString());
        }
        combo.select(selectedIndex);
    }

    protected void fillFields(Combo combo, SimpleFeatureType schema, FieldType fieldType) {
        combo.removeAll();
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }

            Class<?> binding = descriptor.getType().getBinding();
            switch (fieldType) {
            case ALL:
                combo.add(descriptor.getLocalName());
                break;
            case Double:
                if (Double.class.isAssignableFrom(binding) || Float.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Integer:
                if (Short.class.isAssignableFrom(binding)
                        || Integer.class.isAssignableFrom(binding)
                        || Long.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Number:
                if (Number.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case String:
                if (String.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            }
        }
    }
}
