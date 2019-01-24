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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Abstract Binning Operation.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public abstract class BinningOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(BinningOperation.class);

    protected static final String UID = "uid";

    protected static final String AGG_FIELD = "val";

    protected static int quadrantSegments = 16;

    private Boolean onlyValidGrid = Boolean.TRUE;

    public Boolean getOnlyValidGrid() {
        return onlyValidGrid;
    }

    public void setOnlyValidGrid(Boolean onlyValidGrid) {
        this.onlyValidGrid = onlyValidGrid;
    }

    protected MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS, boolean lenient) {
        if (targetCRS == null || CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            return null;
        }

        try {
            return CRS.findMathTransform(sourceCRS, targetCRS, lenient);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Could not create math transform");
        }
    }

    protected Geometry transform(GeometryCoordinateSequenceTransformer transformer, Geometry source) {
        try {
            return transformer.transform(source);
        } catch (TransformException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return source;
    }
}
