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
package org.geotools.process.spatialstatistics.distribution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Identifies the geographic center (or the center of concentration) for a set of features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MeanCenterOperation extends AbstractDisributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(MeanCenterOperation.class);

    final String TYPE_NAME = "MeanCenter";

    final String[] FIELDS = { "XCoord", "YCoord" };

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String weightField,
            String caseField, String dimensionField) throws IOException {
        SimpleFeatureType schema = features.getSchema();
        weightField = FeatureTypes.validateProperty(schema, weightField);
        caseField = FeatureTypes.validateProperty(schema, caseField);
        dimensionField = FeatureTypes.validateProperty(schema, dimensionField);

        int idxWeight = weightField == null ? -1 : schema.indexOf(weightField);
        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);
        int idxDim = dimensionField == null ? -1 : schema.indexOf(dimensionField);
        Expression weightExpr = ff.property(weightField);
        Expression dimensionExpr = ff.property(dimensionField);

        MeanCenterVisitor visitor = new MeanCenterVisitor();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Coordinate coordinate = getTrueCentroid(geometry);
                Object caseVal = idxCase == -1 ? ALL : feature.getAttribute(idxCase);

                double weightVal = 1.0;
                if (idxWeight != -1) {
                    weightVal = this.getValue(feature, weightExpr, weightVal);
                }

                double dimVal = Double.NaN;
                if (idxDim != -1) {
                    dimVal = this.getValue(feature, dimensionExpr, dimVal);
                }

                visitor.visit(coordinate, caseVal, weightVal, dimVal);
            }
        } finally {
            featureIter.close();
        }

        // build feature collection
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        String geomName = schema.getGeometryDescriptor().getLocalName();

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName,
                Point.class, crs);
        featureType = FeatureTypes.add(featureType, FIELDS[0], Double.class, 38);
        featureType = FeatureTypes.add(featureType, FIELDS[1], Double.class, 38);

        if (idxCase != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(caseField));
        }
        if (idxDim != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(dimensionField));
        }

        ListFeatureCollection meanCenterFC = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        int featureID = 0;
        @SuppressWarnings("unchecked")
        HashMap<Object, MeanCenter> resultMap = visitor.getResult();
        Iterator<Object> iterator = resultMap.keySet().iterator();
        while (iterator.hasNext()) {
            Object caseVal = iterator.next();
            MeanCenter meanCenter = resultMap.get(caseVal);
            Point centroid = meanCenter.getMeanCenter();

            // create feature and set geometry
            builder.reset();
            SimpleFeature newFeature = builder.buildFeature(TYPE_NAME + "." + ++featureID);
            newFeature.setDefaultGeometry(centroid);
            newFeature.setAttribute(FIELDS[0], centroid.getX());
            newFeature.setAttribute(FIELDS[1], centroid.getY());

            if (idxCase != -1) {
                newFeature.setAttribute(caseField, caseVal);
            }

            if (idxDim != -1) {
                newFeature.setAttribute(dimensionField, meanCenter.getDimension());
            }

            meanCenterFC.add(newFeature);
        }

        return meanCenterFC;
    }
}
