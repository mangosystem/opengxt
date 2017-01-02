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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsField;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.SummaryFieldBuilder;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Point Statistics Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointStatisticsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PointStatisticsOperation.class);

    // FIRST, LAST, SUM, MEAN, MIN, MAX, RANGE, STD, VAR, COUNT

    private double bufferDistance = 0.0d;

    public double getBufferDistance() {
        return bufferDistance;
    }

    public void setBufferDistance(double bufferDistance) {
        this.bufferDistance = bufferDistance;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygons, String cntField,
            String summaryFields, SimpleFeatureCollection points) throws IOException {
        return execute(polygons, cntField, summaryFields, null, points);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygons, String cntField,
            String summaryFields, String targetFields, SimpleFeatureCollection points)
            throws IOException {
        // create schema
        boolean hasCountField = cntField != null && cntField.length() > 0;
        String typeName = polygons.getSchema().getTypeName();
        SimpleFeatureType schema = FeatureTypes.build(polygons.getSchema(), typeName);
        if (hasCountField) {
            schema = FeatureTypes.add(schema, cntField, Integer.class);
        }

        SummaryFieldBuilder sfBuilder = new SummaryFieldBuilder();
        List<StatisticsField> statFields = null;
        if (targetFields == null || targetFields.isEmpty()) {
            statFields = sfBuilder.buildFields(points.getSchema(), summaryFields);
        } else {
            statFields = sfBuilder.buildFields(points.getSchema(), summaryFields, targetFields);
        }

        if (statFields == null) {
            throw new InvalidParameterValueException("Invalid parameters", "summaryFields",
                    summaryFields);
        }

        schema = addAttributes(schema, statFields);

        // build unique fields
        final List<String> uvFields = new ArrayList<String>();
        for (StatisticsField curField : statFields) {
            if (!uvFields.contains(curField.getSrcField())) {
                uvFields.add(curField.getSrcField());
            }
        }

        // check CRS
        CoordinateReferenceSystem aCrs = polygons.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem bCrs = points.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(aCrs, bCrs) && bCrs != null) {
            // reproject joinFeatures to inputFeatures CRS
            points = new ReprojectingFeatureCollection(points, aCrs);
        }

        // use SpatialIndexFeatureCollection
        points = DataUtils.toSpatialIndexFeatureCollection(points);

        // prepare transactional feature store
        final String the_geom = points.getSchema().getGeometryDescriptor().getLocalName();
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        SimpleFeatureIterator featureIter = polygons.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                MultipleStatVisitor visitor = new MultipleStatVisitor(points.getSchema());
                if (bufferDistance > 0) {
                    geometry = geometry.buffer(bufferDistance);
                }

                Filter filter = getIntersectsFilter(the_geom, geometry);

                int featureCount = 0;
                if (statFields.size() > 0) {
                    SimpleFeatureIterator pointIter = null;
                    try {
                        pointIter = points.subCollection(filter).features();
                        while (pointIter.hasNext()) {
                            SimpleFeature pointFeature = pointIter.next();
                            featureCount++;
                            for (String srcField : uvFields) {
                                visitor.visit(srcField, pointFeature);
                            }
                        }
                    } finally {
                        pointIter.close();
                    }
                } else {
                    featureCount = points.subCollection(filter).size();
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                if (hasCountField) {
                    newFeature.setAttribute(cntField, featureCount);
                }

                if (visitor.getResult().size() > 0) {
                    HashMap<Object, StatisticsVisitor> stat = visitor.getResult();
                    for (StatisticsField field : statFields) {
                        StatisticsVisitorResult ret = stat.get(field.getSrcField()).getResult();
                        Object value = ret.getValue(field.getStatType());
                        newFeature.setAttribute(field.getTargetField(), value);
                    }
                }

                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private SimpleFeatureType addAttributes(SimpleFeatureType schema,
            List<StatisticsField> attributes) {
        for (StatisticsField sfield : attributes) {
            schema = FeatureTypes.add(schema, sfield.getTargetField(), sfield.fieldType,
                    sfield.getFieldLength());
        }

        return schema;
    }

    static final class MultipleStatVisitor {

        static final String CASE_ALL = "ALL";

        SimpleFeatureType schema;

        HashMap<Object, StatisticsVisitor> resuleMap = new LinkedHashMap<Object, StatisticsVisitor>();

        public MultipleStatVisitor(SimpleFeatureType schema) {
            this.schema = schema;
        }

        public HashMap<Object, StatisticsVisitor> getResult() {
            return resuleMap;
        }

        public void visit(String propertyName, SimpleFeature feature) {
            propertyName = propertyName == null ? CASE_ALL : propertyName;
            StatisticsVisitor visitor = resuleMap.get(propertyName);

            if (visitor == null) {
                visitor = new StatisticsVisitor(schema, propertyName);
                resuleMap.put(propertyName, visitor);
            }

            visitor.visit(feature);
        }
    }

}
