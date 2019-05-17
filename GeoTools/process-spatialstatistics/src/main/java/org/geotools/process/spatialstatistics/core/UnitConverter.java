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

import java.util.logging.Logger;

import javax.measure.Unit;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;

import org.geotools.measure.Measure;
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import si.uom.SI;
import systems.uom.common.USCustomary;

/**
 * Utility class for unit conversion
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class UnitConverter {
    protected static final Logger LOGGER = Logging.getLogger(UnitConverter.class);

    public static double convertDistance(double value, DistanceUnit valueUnit,
            Unit<Length> targetUnit) {
        javax.measure.UnitConverter converter = null;

        switch (valueUnit) {
        case Default:
            return value;
        case Meters:
            converter = SI.METRE.getConverterTo(targetUnit);
            break;
        case Kilometers:
            converter = SI.METRE.multiply(1000).getConverterTo(targetUnit);
            break;
        case Feet:
            converter = USCustomary.FOOT.getConverterTo(targetUnit);
            break;
        case Inches:
            converter = USCustomary.INCH.getConverterTo(targetUnit);
            break;
        case Miles:
            converter = USCustomary.MILE.getConverterTo(targetUnit);
            break;
        case NauticalMiles:
            converter = USCustomary.NAUTICAL_MILE.getConverterTo(targetUnit);
            break;
        case Yards:
            converter = USCustomary.YARD.getConverterTo(targetUnit);
            break;
        default:
            return value;
        }

        return converter.convert(value);
    }

    @SuppressWarnings("unchecked")
    public static double convertDistance(Measure measure, DistanceUnit targetUnit) {
        Unit<Length> sourceUnit = (Unit<Length>) measure.getUnit();

        javax.measure.UnitConverter converter = null;
        switch (targetUnit) {
        case Default:
            return measure.doubleValue();
        case Meters:
            converter = sourceUnit.getConverterTo(SI.METRE);
            break;
        case Kilometers:
            converter = sourceUnit.getConverterTo(SI.METRE.multiply(1000));
            break;
        case Feet:
            converter = sourceUnit.getConverterTo(USCustomary.FOOT);
            break;
        case Inches:
            converter = sourceUnit.getConverterTo(USCustomary.INCH);
            break;
        case Miles:
            converter = sourceUnit.getConverterTo(USCustomary.MILE);
            break;
        case NauticalMiles:
            converter = sourceUnit.getConverterTo(USCustomary.NAUTICAL_MILE);
            break;
        case Yards:
            converter = sourceUnit.getConverterTo(USCustomary.YARD);
            break;
        default:
            return measure.doubleValue();
        }

        return converter.convert(measure.doubleValue());
    }

    @SuppressWarnings("unchecked")
    public static double convertArea(Measure measure, AreaUnit targetUnit) {
        Unit<Area> sourceUnit = (Unit<Area>) measure.getUnit();

        javax.measure.UnitConverter converter = null;
        switch (targetUnit) {
        case Default:
            return measure.doubleValue();
        case Acre:
            converter = sourceUnit.getConverterTo(USCustomary.ACRE);
            break;
        case Hectare:
            converter = sourceUnit.getConverterTo(USCustomary.HECTARE);
            break;
        case SquareFeet:
            converter = sourceUnit.getConverterTo(USCustomary.SQUARE_FOOT);
            break;
        case SquareKilometers:
            Unit<Area> sqKilos = (Unit<Area>) SI.SQUARE_METRE.multiply(1000000);
            converter = sourceUnit.getConverterTo(sqKilos);
            break;
        case SquareMeters:
            converter = sourceUnit.getConverterTo(SI.SQUARE_METRE);
            break;
        case SquareMiles:
            Unit<Area> sqMiles = (Unit<Area>) USCustomary.MILE.multiply(USCustomary.MILE);
            converter = sourceUnit.getConverterTo(sqMiles);
            break;
        case SquareYards:
            Unit<Area> sqYards = (Unit<Area>) USCustomary.YARD.multiply(USCustomary.YARD);
            converter = sourceUnit.getConverterTo(sqYards);
            break;
        default:
            return measure.doubleValue();
        }

        return converter.convert(measure.doubleValue());
    }

    @SuppressWarnings("unchecked")
    public static Unit<Length> getLengthUnit(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(crs);
        if (horCRS instanceof GeographicCRS) {
            return SI.METRE; // default
        } else {
            return (Unit<Length>) horCRS.getCoordinateSystem().getAxis(0).getUnit();
        }
    }

    @SuppressWarnings("unchecked")
    public static Unit<Area> getAreaUnit(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(crs);
        if (horCRS instanceof GeographicCRS) {
            return SI.SQUARE_METRE; // default
        } else {
            Unit<?> distUnit = horCRS.getCoordinateSystem().getAxis(0).getUnit();
            return (Unit<Area>) distUnit.multiply(distUnit);
        }
    }
}
