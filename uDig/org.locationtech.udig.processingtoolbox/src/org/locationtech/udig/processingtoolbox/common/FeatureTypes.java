/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.common;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.IsLessThenOrEqualToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * FeatureType Utilities
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class FeatureTypes {
    protected static final Logger LOGGER = Logging.getLogger(FeatureTypes.class);

    public static String SHAPE_FIELD = "geom"; //$NON-NLS-1$
    
    public static String NAMESPACE = "feature"; //$NON-NLS-1$

    public static String NAMESPACE_URL = "http://www.mangosystem.com"; //$NON-NLS-1$

    public static String VERSION = "1.0.0"; //$NON-NLS-1$

    public enum SimpleShapeType {
        POINT, LINESTRING, POLYGON
    }

    public static SimpleShapeType getSimpleShapeType(SimpleFeatureCollection features) {
        return getSimpleShapeType(features.getSchema());
    }

    public static SimpleShapeType getSimpleShapeType(SimpleFeatureType schema) {
        return getSimpleShapeType(schema.getGeometryDescriptor().getType().getBinding());
    }

    public static SimpleShapeType getSimpleShapeType(Class<?> geomBinding) {
        if (Polygon.class.equals(geomBinding) || MultiPolygon.class.equals(geomBinding)) {
            return SimpleShapeType.POLYGON;
        } else if (LineString.class.equals(geomBinding)
                || MultiLineString.class.equals(geomBinding)) {
            return SimpleShapeType.LINESTRING;
        } else if (Point.class.equals(geomBinding) || MultiPoint.class.equals(geomBinding)) {
            return SimpleShapeType.POINT;
        }

        return SimpleShapeType.POINT;
    }

    public static int getFID(SimpleFeature sFeature) {
        String fid = sFeature.getID();
        if (fid.contains(".")) {
            final int pos = fid.lastIndexOf(".");
            fid = fid.substring(pos + 1);
        }

        int FID = -1;
        try {
            FID = Integer.parseInt(fid);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        return FID;
    }

    public static boolean isSupportedShapefile(PropertyType type) {
        String supported[] = { "String", "Integer", "Double", "Boolean", "Date", "LineString",
                "MultiLineString", "Polygon", "MultiPolygon", "Point", "MultiPoint" };

        for (String iter : supported) {
            if (type.getBinding().getSimpleName().equalsIgnoreCase(iter)) {
                return true;
            }
        }

        return false;
    }

    public static int getAttributeLength(AttributeDescriptor descriptor) {
        int fieldLength = 255; // String default

        List<Filter> filterList = descriptor.getType().getRestrictions();
        for (Filter filter : filterList) {
            if (filter instanceof IsLessThenOrEqualToImpl) {
                IsLessThenOrEqualToImpl ite = (IsLessThenOrEqualToImpl) filter;
                LiteralExpressionImpl exp = (LiteralExpressionImpl) ite.getExpression2();

                try {
                    fieldLength = Integer.valueOf(exp.getValue().toString());
                    break;
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        return fieldLength;
    }

    public static boolean isNumeric(SimpleFeatureType schema, String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return false;
        }

        AttributeDescriptor atDesc = schema.getDescriptor(propertyName);
        Class<?> binding = atDesc.getType().getBinding();

        return Number.class.isAssignableFrom(binding);
    }

    public static SimpleFeatureType add(SimpleFeatureType schema, AttributeDescriptor descriptor) {
        if (existProeprty(schema, descriptor.getLocalName())) {
            return schema;
        }

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(schema);

        typeBuilder.add(descriptor);

        return typeBuilder.buildFeatureType();
    }

    public static SimpleFeatureType add(SimpleFeatureType schema, String propertyName,
            Class<?> attributeBinding) {
        return add(schema, propertyName, attributeBinding, 0);
    }

    public static boolean existProeprty(SimpleFeatureType schema, String propertyName) {
        propertyName = FeatureTypes.validateProperty(schema, propertyName);
        return schema.indexOf(propertyName) != -1;
    }

    public static SimpleFeatureType add(SimpleFeatureType schema, String propertyName,
            Class<?> attributeBinding, int length) {
        if (existProeprty(schema, propertyName)) {
            return schema;
        }

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(schema);

        if (attributeBinding.isAssignableFrom(String.class)) {
            if (length == 0) {
                length = 255; // string default length
            }
            typeBuilder.length(length).add(propertyName, attributeBinding);
        } else {
            typeBuilder.add(propertyName, attributeBinding);
        }

        return typeBuilder.buildFeatureType();
    }

    public static SimpleFeatureType build(SimpleFeatureCollection features, String typeName) {
        return build(features.getSchema(), typeName);
    }

    public static SimpleFeatureType build(SimpleFeatureType schema, String typeName) {
        return build(schema, typeName, schema.getGeometryDescriptor().getType().getBinding());
    }

    public static SimpleFeatureType build(SimpleFeatureType schema, String typeName,
            Class<?> geometryBinding) {
        return build(schema, typeName, geometryBinding, schema.getCoordinateReferenceSystem());
    }

    public static SimpleFeatureType build(SimpleFeatureType schema, String typeName,
            CoordinateReferenceSystem crs) {
        Class<?> geometryBinding = schema.getGeometryDescriptor().getType().getBinding();

        return build(schema, typeName, geometryBinding, crs);
    }

    public static SimpleFeatureType build(SimpleFeatureType schema, Class<?> geometryBinding) {
        return build(schema, schema.getTypeName(), geometryBinding);
    }

    public static SimpleFeatureType build(SimpleFeatureType schema, String typeName,
            Class<?> geometryBinding, CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();

        final String namespaceURI = schema.getName().getNamespaceURI();
        if (namespaceURI != null) {
            typeBuilder.setNamespaceURI(namespaceURI);
        } else {
            typeBuilder.setNamespaceURI(NAMESPACE_URL);
        }
        typeBuilder.setName(typeName);
        typeBuilder.setCRS(crs);

        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                GeometryDescriptor geomDesc = (GeometryDescriptor) descriptor;

                // MemoryDatastore의 경우 polygon 또는 linestring일 경우 GML 인코딩 오류
                if (geometryBinding.isAssignableFrom(Polygon.class)) {
                    geometryBinding = MultiPolygon.class;
                } else if (geometryBinding.isAssignableFrom(LineString.class)) {
                    geometryBinding = MultiLineString.class;
                }
                typeBuilder.add(geomDesc.getLocalName(), geometryBinding, crs);
            } else {
                typeBuilder.add(descriptor);
            }
        }

        return typeBuilder.buildFeatureType();
    }

    public static SimpleFeatureType getDefaultType(String typeName, Class<?> geometryBinding,
            CoordinateReferenceSystem crs) {
        return getDefaultType(typeName, SHAPE_FIELD, geometryBinding, crs);
    }

    public static SimpleFeatureType getDefaultType(String typeName, String shapeFieldName,
            Class<?> geometryBinding, CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setNamespaceURI(NAMESPACE_URL);
        typeBuilder.setName(typeName);
        typeBuilder.setCRS(crs);

        // MemoryDatastore의 경우 polygon 또는 linestring일 경우 GML 인코딩 오류
        if (geometryBinding.isAssignableFrom(Polygon.class)) {
            geometryBinding = MultiPolygon.class;
        } else if (geometryBinding.isAssignableFrom(LineString.class)) {
            geometryBinding = MultiLineString.class;
        }

        // create shape field
        typeBuilder.add(shapeFieldName, geometryBinding, crs);

        return typeBuilder.buildFeatureType();
    }

    public static boolean equalGeometryType(SimpleFeatureType origSchema,
            SimpleFeatureType refSchema) {
        Class<?> origBinding = origSchema.getGeometryDescriptor().getType().getBinding();
        Class<?> refBinding = origSchema.getGeometryDescriptor().getType().getBinding();

        boolean isEqual = false;
        if (origBinding.isAssignableFrom(MultiPolygon.class)) {
            isEqual = refBinding.isAssignableFrom(Polygon.class)
                    || refBinding.isAssignableFrom(MultiPolygon.class);
        } else if (origBinding.isAssignableFrom(Polygon.class)) {
            isEqual = refBinding.isAssignableFrom(Polygon.class)
                    || refBinding.isAssignableFrom(MultiPolygon.class);
        } else if (origBinding.isAssignableFrom(MultiLineString.class)) {
            isEqual = refBinding.isAssignableFrom(LineString.class)
                    || refBinding.isAssignableFrom(MultiLineString.class);
        } else if (origBinding.isAssignableFrom(LineString.class)) {
            isEqual = refBinding.isAssignableFrom(LineString.class)
                    || refBinding.isAssignableFrom(MultiLineString.class);
        } else if (origBinding.isAssignableFrom(MultiPoint.class)) {
            isEqual = refBinding.isAssignableFrom(Point.class)
                    || refBinding.isAssignableFrom(MultiPoint.class);
        } else if (origBinding.isAssignableFrom(Point.class)) {
            isEqual = refBinding.isAssignableFrom(Point.class)
                    || refBinding.isAssignableFrom(MultiPoint.class);
        }

        return isEqual;
    }

    public static boolean eaualPropertys(SimpleFeatureType source, SimpleFeatureType target) {
        for (AttributeDescriptor descriptor : source.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }

            String propertyName = validateProperty(target, descriptor.getLocalName());
            if (target.indexOf(propertyName) == -1) {
                return false;
            }
        }
        return true;
    }

    public static String validateProperty(SimpleFeatureType schema, String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return null;
        }

        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor.getLocalName().equalsIgnoreCase(propertyName)) {
                return descriptor.getLocalName();
            }
        }
        return propertyName;
    }

}