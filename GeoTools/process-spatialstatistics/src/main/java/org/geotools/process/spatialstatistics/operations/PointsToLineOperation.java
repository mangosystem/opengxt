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
import java.util.Hashtable;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.util.BezierCurve;
import org.geotools.process.spatialstatistics.util.GeodeticBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Creates line features from points.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PointsToLineOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(PointsToLineOperation.class);

    private static final String length = "length";

    private boolean closeLine = false;

    private String lineField = null;

    private boolean useBezierCurve = false;

    private BezierCurve bezierCurve = null;

    private boolean geodesicLine = false;

    private GeodeticBuilder geodetic = null;

    public boolean isCloseLine() {
        return closeLine;
    }

    public void setCloseLine(boolean closeLine) {
        this.closeLine = closeLine;
    }

    public boolean isUseBezierCurve() {
        return useBezierCurve;
    }

    public void setUseBezierCurve(boolean useBezierCurve) {
        this.useBezierCurve = useBezierCurve;
    }

    public boolean isGeodesicLine() {
        return geodesicLine;
    }

    public void setGeodesicLine(boolean geodesicLine) {
        this.geodesicLine = geodesicLine;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String lineField,
            String sortField) throws IOException {
        return execute(inputFeatures, lineField, sortField, this.closeLine);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String lineField,
            String sortField, boolean closeLine) throws IOException {
        return execute(inputFeatures, lineField, sortField, closeLine, useBezierCurve);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String lineField,
            String sortField, boolean closeLine, boolean useBezierCurve) throws IOException {
        this.closeLine = closeLine;
        this.useBezierCurve = useBezierCurve;
        if (useBezierCurve) {
            bezierCurve = new BezierCurve();
            bezierCurve.setUseSegment(true);
        }

        // prepare feature type
        SimpleFeatureType inputSchema = inputFeatures.getSchema();
        String typeName = inputSchema.getTypeName();
        CoordinateReferenceSystem crs = inputSchema.getCoordinateReferenceSystem();
        String geomName = inputFeatures.getSchema().getGeometryDescriptor().getLocalName();

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(typeName, geomName,
                LineString.class, crs);
        if (closeLine) {
            featureType = FeatureTypes.getDefaultType(typeName, geomName, Polygon.class, crs);
        }

        if (geodesicLine && UnitConverter.isGeographicCRS(crs)) {
            geodetic = new GeodeticBuilder(crs);
        }

        boolean hasLineField = lineField != null && lineField.length() > 0;
        boolean hasSortField = sortField != null && sortField.length() > 0;

        if (hasLineField) {
            lineField = FeatureTypes.validateProperty(inputSchema, lineField);
            hasLineField = inputSchema.indexOf(lineField) != -1;
            if (hasLineField) {
                featureType = FeatureTypes.add(featureType, inputSchema.getDescriptor(lineField));
                this.lineField = lineField;
            }
        }
        featureType = FeatureTypes.add(featureType, length, Double.class, 33);

        if (hasSortField) {
            sortField = FeatureTypes.validateProperty(inputSchema, sortField);
            hasSortField = inputSchema.indexOf(sortField) != -1;
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        try {
            SimpleFeatureCollection subCollection = inputFeatures;
            if (hasLineField) {
                Hashtable<Object, Object> uvValues = getUniqueValues(inputFeatures, lineField);
                for (Object uvValue : uvValues.keySet()) {
                    Filter filter = ff.equal(ff.property(lineField), ff.literal(uvValue), true);
                    subCollection = inputFeatures.subCollection(filter);
                    if (hasSortField) {
                        SortBy sort = ff.sort(sortField, SortOrder.ASCENDING);
                        insertFeatures(subCollection.sort(sort), uvValue, featureWriter);
                    } else {
                        insertFeatures(subCollection, uvValue, featureWriter);
                    }
                }
            } else {
                if (hasSortField) {
                    subCollection = inputFeatures.sort(ff.sort(sortField, SortOrder.ASCENDING));
                }
                insertFeatures(subCollection, null, featureWriter);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(null);
        }

        return featureWriter.getFeatureCollection();
    }

    private void insertFeatures(SimpleFeatureCollection inputFeatures, Object lineValue,
            IFeatureInserter featureWriter) throws IOException {
        CoordinateList coordinates = new CoordinateList();

        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                coordinates.add(geometry.getCoordinate(), false);
            }
        } finally {
            featureIter.close();
        }

        if ((coordinates.size() <= 1) || (closeLine && coordinates.size() < 3)) {
            return;
        }

        if (coordinates.size() > 1) {
            // create feature and set geometry
            if (closeLine) {
                if (!coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
                    coordinates.add(coordinates.get(0), false);
                }
            }

            LineString line = gf.createLineString(coordinates.toCoordinateArray());
            if (useBezierCurve && false == geodesicLine) {
                line = bezierCurve.create(line);
            }

            Geometry geometry = line;
            if (geodesicLine && geodetic != null) {
                geometry = geodetic.toGeodesicLine(line);
            }

            if (geometry == null || geometry.isEmpty()) {
                return;
            }

            if (closeLine) {
                LinearRing ring = gf.createLinearRing(coordinates.toCoordinateArray());
                geometry = gf.createPolygon(ring);
            }

            SimpleFeature newFeature = featureWriter.buildFeature();
            newFeature.setDefaultGeometry(geometry);
            if (lineValue != null && lineField != null) {
                newFeature.setAttribute(lineField, lineValue);
            }
            newFeature.setAttribute(length, geometry.getLength());
            featureWriter.write(newFeature);
        }
    }

    private Hashtable<Object, Object> getUniqueValues(SimpleFeatureCollection inputFeatures,
            String lineField) {
        Hashtable<Object, Object> table = new Hashtable<Object, Object>();
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            Expression expression = ff.property(lineField);
            while (featureIter.hasNext()) {
                Object value = expression.evaluate(featureIter.next());
                if (value == null) {
                    // skip null value feature
                    continue;
                }
                table.put(value, value);
            }
        } finally {
            featureIter.close();
        }
        return table;
    }
}
