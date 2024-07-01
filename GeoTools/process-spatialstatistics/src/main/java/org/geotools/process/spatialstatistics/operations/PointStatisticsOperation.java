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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.parameter.InvalidParameterValueException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.StatisticsField;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.core.SummaryFieldBuilder;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

import si.uom.SI;

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

    private DistanceUnit distanceUnit = DistanceUnit.Default;

    private GeodeticBuilder geodetic;

    private int quadrantSegments = 12; // JTS default = 8

    public double getBufferDistance() {
        return bufferDistance;
    }

    public void setBufferDistance(double bufferDistance) {
        this.bufferDistance = bufferDistance;
    }

    public int getQuadrantSegments() {
        return quadrantSegments;
    }

    public void setQuadrantSegments(int quadrantSegments) {
        if (quadrantSegments <= 2) {
            quadrantSegments = 2;
        }
        this.quadrantSegments = quadrantSegments;
    }

    public DistanceUnit getDistanceUnit() {
        return distanceUnit;
    }

    public void setDistanceUnit(DistanceUnit distanceUnit) {
        this.distanceUnit = distanceUnit;
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
        // check coordinate reference system
        CoordinateReferenceSystem crsT = polygons.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = points.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            points = new ReprojectFeatureCollection(points, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        boolean isGeographicCRS = UnitConverter.isGeographicCRS(crsT);
        if (isGeographicCRS) {
            geodetic = new GeodeticBuilder(crsT);
            geodetic.setQuadrantSegments(quadrantSegments);
        }

        double radius = bufferDistance;
        if (distanceUnit != DistanceUnit.Default) {
            Unit<Length> targetUnit = UnitConverter.getLengthUnit(crsT);
            if (isGeographicCRS) {
                radius = UnitConverter.convertDistance(radius, distanceUnit, SI.METRE);
            } else {
                radius = UnitConverter.convertDistance(radius, distanceUnit, targetUnit);
            }
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
                if (radius > 0) {
                    if (distanceUnit != DistanceUnit.Default && isGeographicCRS) {
                        geometry = geodetic.buffer(geometry, radius);
                    } else {
                        geometry = geometry.buffer(radius, quadrantSegments);
                    }
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
