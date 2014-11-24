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
package org.geotools.process.spatialstatistics.storage;

import java.util.ArrayList;
import java.util.List;

import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;

/**
 * FieldMap class
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public final class FieldMap {
    public int soruceID = -1;

    public int destID = -1;

    public String source;

    public String dest;

    public boolean isGeometry = false;

    public Object userData;

    public void mappingField(SimpleFeatureType sourceType, SimpleFeatureType destType,
            String destPropertyName) {
        isGeometry = false;
        destPropertyName = FeatureTypes.validateProperty(destType, destPropertyName);
        destID = destType.indexOf(destPropertyName);

        for (AttributeDescriptor attr : sourceType.getAttributeDescriptors()) {
            String attributeName = attr.getLocalName();
            if (attributeName.equalsIgnoreCase(destPropertyName)) {
                soruceID = sourceType.indexOf(attributeName);
                if (attr.getType() instanceof GeometryType) {
                    isGeometry = true;
                }
                return;
            }
        }
    }

    public static List<FieldMap> buildMap(SimpleFeatureType sourceType, SimpleFeatureType destType) {
        List<FieldMap> fieldMaps = new ArrayList<FieldMap>();

        int geometryProperty = 0;
        for (AttributeDescriptor desdAttr : destType.getAttributeDescriptors()) {
            FieldMap fieldMap = new FieldMap();
            fieldMap.mappingField(sourceType, destType, desdAttr.getLocalName());
            if (fieldMap.soruceID != -1 && fieldMap.destID != -1) {
                if (fieldMap.isGeometry) {
                    geometryProperty++;
                }
                fieldMaps.add(fieldMap);
            }
        }

        // 두 스키마의 Geometry 필드 이름이 다르다.
        if (geometryProperty == 0) {
            String sgeom = sourceType.getGeometryDescriptor().getLocalName();
            String dgeom = destType.getGeometryDescriptor().getLocalName();

            FieldMap fieldMap = new FieldMap();
            fieldMap.soruceID = sourceType.indexOf(sgeom);
            fieldMap.destID = destType.indexOf(dgeom);
            fieldMaps.add(fieldMap);
        }

        return fieldMaps;
    }
}
