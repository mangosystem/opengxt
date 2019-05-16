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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.operation.TransformException;

/**
 * Abstract Binning Visitor.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public abstract class AbstractBinningVisitor {
    protected static final Logger LOGGER = Logging.getLogger(AbstractBinningVisitor.class);

    protected final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    private Boolean onlyValidGrid = Boolean.TRUE;

    protected ReferencedEnvelope extent;

    protected int columns;

    protected int rows;

    protected Double[][] gridValues;

    protected Geometry binTemplate;

    protected int minCol = Integer.MAX_VALUE;

    protected int minRow = Integer.MAX_VALUE;

    protected int maxCol = Integer.MIN_VALUE;

    protected int maxRow = Integer.MIN_VALUE;

    protected double minX;

    protected double minY;

    public abstract void visit(Coordinate coordinate, double value);

    protected void visit(Geometry point, double value) {
        this.visit(point.getCentroid().getCoordinate(), value);
    }

    protected void visit(SimpleFeatureCollection features, Expression weight,
            GeometryCoordinateSequenceTransformer transformer) {
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature featture = featureIter.next();
                Double value = weight == null ? Double.valueOf(1.0) : weight.evaluate(featture,
                        Double.class);
                if (value == null) {
                    continue;
                }

                Geometry geometry = (Geometry) featture.getDefaultGeometry();
                if (transformer != null) {
                    // project source geometry to targetCRS
                    geometry = transform(transformer, geometry);
                }

                this.visit(geometry, value);
            }
        } finally {
            featureIter.close();
        }
    }

    public abstract Iterator<Bin> getBins(GeometryCoordinateSequenceTransformer transformer);

    public Boolean getOnlyValidGrid() {
        return onlyValidGrid;
    }

    public void setOnlyValidGrid(Boolean onlyValidGrid) {
        this.onlyValidGrid = onlyValidGrid;
    }

    protected Geometry transform(GeometryCoordinateSequenceTransformer transformer, Geometry source) {
        try {
            return transformer.transform(source);
        } catch (TransformException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return source;
    }

    public static final class Bin {
        public int featureID = 0;

        public Geometry geometry = null;

        public Double value = null;

        public Bin(int featureID, Geometry geometry, Double value) {
            this.featureID = featureID;
            this.geometry = geometry;
            this.value = value;
        }
    }
}
