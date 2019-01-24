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
package org.geotools.process.spatialstatistics.pattern;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.pattern.AbstractBinningVisitor.Bin;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Performs hexagonal binning.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class HexagonalBinningOperation extends BinningOperation {
    protected static final Logger LOGGER = Logging.getLogger(HexagonalBinningOperation.class);

    static final String TYPE_NAME = "HexagonalBinning";

    public HexagonalBinningOperation() {

    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, ReferencedEnvelope bbox)
            throws IOException {
        double size = Math.min(bbox.getWidth(), bbox.getHeight()) / 15.0;
        return execute(features, null, bbox, size);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            ReferencedEnvelope bbox, double size) throws IOException {
        return execute(features, null, bbox, size);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox) throws IOException {
        double size = Math.min(bbox.getWidth(), bbox.getHeight()) / 15.0;
        return execute(features, null, bbox, size);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, Expression weight,
            ReferencedEnvelope bbox, double size) throws IOException {
        if (bbox == null) {
            throw new NullPointerException("bbox parameter is null");
        }

        // check crs
        CoordinateReferenceSystem sourceCRS = features.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = bbox.getCoordinateReferenceSystem();
        MathTransform transform = findMathTransform(sourceCRS, targetCRS, true);

        GeometryCoordinateSequenceTransformer transformer = null;
        if (transform != null) {
            transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(transform);
            transformer.setCoordinateReferenceSystem(targetCRS);
        }

        // calculate
        HexagonalBinningVisitor visitor = new HexagonalBinningVisitor(bbox, size);
        visitor.setOnlyValidGrid(getOnlyValidGrid());
        visitor.visit(features, weight, transformer);

        // create feature type
        SimpleFeatureType schema = FeatureTypes.getDefaultType(TYPE_NAME, Polygon.class, sourceCRS);
        schema = FeatureTypes.add(schema, UID, Integer.class, 19);
        schema = FeatureTypes.add(schema, AGG_FIELD, Double.class, 38);

        // write features
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        try {
            if (transformer != null) {
                transformer.setMathTransform(transform.inverse());
                transformer.setCoordinateReferenceSystem(sourceCRS);
            }

            Iterator<Bin> iter = visitor.getBins(transformer);
            while (iter.hasNext()) {
                Bin bin = iter.next();

                Geometry grid = bin.geometry;
                grid.setUserData(targetCRS);

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setAttribute(UID, bin.featureID);
                newFeature.setAttribute(AGG_FIELD, bin.value);
                newFeature.setDefaultGeometry(grid);

                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
