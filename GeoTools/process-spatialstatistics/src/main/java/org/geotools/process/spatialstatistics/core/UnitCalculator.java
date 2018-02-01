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
package org.geotools.process.spatialstatistics.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Measure;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Utility class for unit calculation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class UnitCalculator {
    protected static final Logger LOGGER = Logging.getLogger(UnitCalculator.class);

    private Unit<Length> distanceUnit = SI.METER; // default

    private Unit<Area> areaUnit = SI.SQUARE_METRE; // default

    private CoordinateReferenceSystem sourceCRS;

    private GeometryCoordinateSequenceTransformer transformer = null;

    private boolean isGeographic = false;

    public static boolean isGeographicCRS(CoordinateReferenceSystem sourceCRS) {
        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(sourceCRS);
        return horCRS instanceof GeographicCRS;
    }

    @SuppressWarnings("unchecked")
    public UnitCalculator(CoordinateReferenceSystem sourceCRS) {
        this.sourceCRS = sourceCRS;

        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(sourceCRS);
        if (horCRS instanceof GeographicCRS) {
            this.distanceUnit = SI.METER; // default
            this.areaUnit = SI.SQUARE_METRE; // default
            this.isGeographic = true;
        } else {
            CoordinateSystem cs = horCRS.getCoordinateSystem();
            this.distanceUnit = (Unit<Length>) cs.getAxis(0).getUnit();
            this.areaUnit = (Unit<Area>) distanceUnit.times(distanceUnit);
        }
    }

    public void setupTransformation(ReferencedEnvelope extent) {
        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(sourceCRS);
        if (isGeographic && (horCRS instanceof GeographicCRS)) {
            try {
                this.distanceUnit = SI.METER; // default
                this.areaUnit = SI.SQUARE_METRE; // default

                // GeographicCRS to UTM
                Coordinate p = extent.centre();
                CoordinateReferenceSystem forcedCRS = CRS.decode("AUTO:42001," + p.x + "," + p.y);

                this.transformer = new GeometryCoordinateSequenceTransformer();
                this.transformer.setMathTransform(transform(sourceCRS, forcedCRS, true));
                this.transformer.setCoordinateReferenceSystem(forcedCRS);
            } catch (NoSuchAuthorityCodeException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
    }

    public boolean isGeographic() {
        return isGeographic;
    }

    public double getArea(Geometry geometry, AreaUnit targetUnit) {
        double area = transformGeometry(geometry).getArea();
        if (targetUnit == AreaUnit.Default) {
            return area;
        }

        return UnitConverter.convertArea(Measure.valueOf(area, areaUnit), targetUnit);
    }

    public double getLength(Geometry geometry, DistanceUnit targetUnit) {
        double length = transformGeometry(geometry).getLength();
        if (targetUnit == DistanceUnit.Default) {
            return length;
        }

        return UnitConverter.convertDistance(Measure.valueOf(length, distanceUnit), targetUnit);
    }

    private Geometry transformGeometry(Geometry geometry) {
        if (isGeographic) {
            try {
                return transformer.transform(geometry);
            } catch (TransformException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        return geometry;
    }

    private MathTransform transform(CoordinateReferenceSystem source,
            CoordinateReferenceSystem target, boolean lenient) {
        try {
            return CRS.findMathTransform(source, target, lenient);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Could not create math transform");
        }
    }
}