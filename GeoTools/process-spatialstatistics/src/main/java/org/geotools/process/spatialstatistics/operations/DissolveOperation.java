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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsField;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.SummaryFieldBuilder;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

/**
 * Dissolves features based on specified attributes and aggregation functions.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DissolveOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(DissolveOperation.class);

    static final String NULL = "NULLVALUE";

    private Boolean useMultiPart = Boolean.TRUE;

    private Hashtable<Object, Hashtable<String, StatisticsVisitor>> attMap;

    private Hashtable<Object, List<Geometry>> geoMap;

    public boolean isUseMultiPart() {
        return useMultiPart;
    }

    public void setUseMultiPart(boolean useMultiPart) {
        this.useMultiPart = useMultiPart;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String dissolveField,
            String summaryFields) throws IOException {
        return execute(features, dissolveField, summaryFields, null);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String dissolveField,
            String summaryFields, String targetFields) throws IOException {
        if (StringHelper.isNullOrEmpty(dissolveField)) {
            throw new NullPointerException("dissolveField parameter required!");
        }

        SimpleFeatureType schema = features.getSchema();
        dissolveField = FeatureTypes.validateProperty(schema, dissolveField);

        // create TransactionalFeatureCollection
        Class<?> binding = schema.getGeometryDescriptor().getType().getBinding();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();

        SummaryFieldBuilder sfBuilder = SummaryFieldBuilder.getInstance();
        List<StatisticsField> statisticsList;
        if (targetFields == null || targetFields.isEmpty()) {
            statisticsList = sfBuilder.buildFields(schema, summaryFields);
        } else {
            statisticsList = sfBuilder.buildFields(schema, summaryFields, targetFields);
        }

        if (statisticsList == null) {
            throw new NullPointerException("cannot build statistics fields!");
        }

        String geomName = schema.getGeometryDescriptor().getLocalName();
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(schema.getTypeName(), geomName,
                binding, crs);
        featureType = FeatureTypes.add(featureType, schema.getDescriptor(dissolveField));
        featureType = addAttributes(featureType, statisticsList);

        // build unique fields
        final List<String> uvFields = new ArrayList<String>();
        for (StatisticsField curField : statisticsList) {
            if (!uvFields.contains(curField.getSrcField())) {
                uvFields.add(curField.getSrcField());
            }
        }

        // calculate statistics
        calculateStatistics(features, dissolveField, schema, uvFields);

        // post process
        IFeatureInserter writer = getFeatureWriter(featureType);
        try {
            for (Entry<Object, List<Geometry>> entry : geoMap.entrySet()) {
                CascadedPolygonUnion unionOp = new CascadedPolygonUnion(entry.getValue());
                Geometry unionGeometry = unionOp.union();

                if (useMultiPart) {
                    // multi part feature
                    SimpleFeature newFeature = writer.buildFeature();
                    newFeature.setDefaultGeometry(unionGeometry);
                    newFeature.setAttribute(dissolveField, entry.getKey());

                    Hashtable<String, StatisticsVisitor> map = attMap.get(entry.getKey());
                    if (map != null) {
                        for (StatisticsField field : statisticsList) {
                            StatisticsVisitorResult ret = map.get(field.getSrcField()).getResult();
                            if (ret == null) {
                                continue;
                            }
                            Object val = ret.getValue(field.getStatType());
                            newFeature.setAttribute(field.getTargetField(), val);
                        }
                    }
                    writer.write(newFeature);
                } else {
                    // single part feature
                    for (int idx = 0; idx < unionGeometry.getNumGeometries(); idx++) {
                        SimpleFeature newFeature = writer.buildFeature();
                        newFeature.setDefaultGeometry(unionGeometry.getGeometryN(idx));
                        newFeature.setAttribute(dissolveField, entry.getKey());

                        Hashtable<String, StatisticsVisitor> map = attMap.get(entry.getKey());
                        if (map != null) {
                            for (StatisticsField field : statisticsList) {
                                StatisticsVisitorResult ret = map.get(field.getSrcField())
                                        .getResult();
                                if (ret == null) {
                                    continue;
                                }
                                Object val = ret.getValue(field.getStatType());
                                newFeature.setAttribute(field.getTargetField(), val);
                            }
                        }
                        writer.write(newFeature);
                    }
                }
            }
        } finally {
            writer.close();
        }

        return writer.getFeatureCollection();
    }

    private void calculateStatistics(SimpleFeatureCollection features, String dissolveField,
            SimpleFeatureType schema, List<String> uvFields) {
        attMap = new Hashtable<Object, Hashtable<String, StatisticsVisitor>>();
        geoMap = new Hashtable<Object, List<Geometry>>();

        SimpleFeatureIterator featureIter = features.features();
        try {
            Expression disExpresion = ff.property(dissolveField);
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Object disValue = disExpresion.evaluate(feature);
                if (disValue == null) {
                    disValue = NULL;
                }

                // geometries
                if (geoMap.get(disValue) == null) {
                    geoMap.put(disValue, new ArrayList<Geometry>());
                }
                geoMap.get(disValue).add(geometry);

                // attributes
                if (attMap.get(disValue) == null) {
                    attMap.put(disValue, new Hashtable<String, StatisticsVisitor>());
                    for (String field : uvFields) {
                        attMap.get(disValue).put(field, new StatisticsVisitor(schema, field));
                    }
                }

                for (String field : uvFields) {
                    attMap.get(disValue).get(field).visit(feature);
                }
            }
        } finally {
            featureIter.close();
        }
    }

    private SimpleFeatureType addAttributes(SimpleFeatureType featureType,
            List<StatisticsField> attributeList) {
        if (attributeList == null) {
            return featureType;
        }

        for (StatisticsField st : attributeList) {
            featureType = FeatureTypes.add(featureType, st.getTargetField(), st.fieldType,
                    st.getFieldLength());
        }
        return featureType;
    }
}
