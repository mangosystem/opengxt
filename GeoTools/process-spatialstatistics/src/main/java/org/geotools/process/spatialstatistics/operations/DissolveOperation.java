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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsField;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.core.SummaryFieldBuilder;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ToPointFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

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
        SimpleFeatureType featureType = FeatureTypes.getDefaultType(getOutputTypeName(), geomName,
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
        Hashtable<Object, Hashtable<String, StatisticsVisitor>> attMap = new Hashtable<Object, Hashtable<String, StatisticsVisitor>>();
        Hashtable<Object, List<Geometry>> geoMap = new Hashtable<Object, List<Geometry>>();

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

        // post process
        IFeatureInserter writer = getFeatureWriter(featureType);
        try {
            int index = 0;
            for (Entry<Object, List<Geometry>> entry : geoMap.entrySet()) {
                CascadedPolygonUnion unionOp = new CascadedPolygonUnion(entry.getValue());
                Geometry unionGeometry = unionOp.union();

                if (useMultiPart) {
                    SimpleFeature newFeature = writer.buildFeature(Integer.toString(++index));
                    newFeature.setDefaultGeometry(unionGeometry);
                    newFeature.setAttribute(dissolveField, entry.getKey());

                    Hashtable<String, StatisticsVisitor> map = attMap.get(entry.getKey());
                    for (StatisticsField field : statisticsList) {
                        StatisticsVisitorResult ret = map.get(field.getSrcField()).getResult();
                        Object val = ret.getValue(field.getStatType());
                        newFeature.setAttribute(field.getTargetField(), val);
                    }
                    writer.write(newFeature);
                } else {
                    SimpleFeatureCollection points = new ToPointFeatureCollection(features, true);
                    String geom = points.getSchema().getGeometryDescriptor().getLocalName();
                    for (int idx = 0; idx < unionGeometry.getNumGeometries(); idx++) {
                        Geometry geometry = unionGeometry.getGeometryN(idx);
                        Filter filter = ff.intersects(ff.property(geom), ff.literal(geometry));

                        // TODO calculate statistics

                        // create feature
                        SimpleFeature newFeature = writer.buildFeature(Integer.toString(++index));
                        newFeature.setDefaultGeometry(geometry);
                        newFeature.setAttribute(dissolveField, entry.getKey());

                        Hashtable<String, StatisticsVisitor> map = attMap.get(entry.getKey());
                        for (StatisticsField field : statisticsList) {
                            StatisticsVisitorResult ret = map.get(field.getSrcField()).getResult();
                            Object val = ret.getValue(field.getStatType());
                            newFeature.setAttribute(field.getTargetField(), val);
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
