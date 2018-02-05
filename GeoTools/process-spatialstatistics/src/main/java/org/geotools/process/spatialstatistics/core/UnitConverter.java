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

import javax.measure.Measure;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

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
        Measure<Double, Length> measure = Measure.valueOf(value, SI.METER);

        switch (valueUnit) {
        case Default:
            return value;
        case Meters:
            measure = Measure.valueOf(value, SI.METER);
            break;
        case Kilometers:
            measure = Measure.valueOf(value, SI.KILOMETER);
            break;
        case Feet:
            measure = Measure.valueOf(value, NonSI.FOOT);
            break;
        case Inches:
            measure = Measure.valueOf(value, NonSI.INCH);
            break;
        case Miles:
            measure = Measure.valueOf(value, NonSI.MILE);
            break;
        case NauticalMiles:
            measure = Measure.valueOf(value, NonSI.NAUTICAL_MILE);
            break;
        case Yards:
            measure = Measure.valueOf(value, NonSI.YARD);
            break;
        default:
            return value;
        }

        return measure.doubleValue(targetUnit);
    }

    public static double convertDistance(Measure<Double, Length> measure, DistanceUnit targetUnit) {
        double converted = measure.getValue();

        switch (targetUnit) {
        case Default:
            return converted;
        case Meters:
            converted = measure.doubleValue(SI.METER);
            break;
        case Kilometers:
            converted = measure.doubleValue(SI.KILOMETER);
            break;
        case Feet:
            converted = measure.doubleValue(NonSI.FOOT);
            break;
        case Inches:
            converted = measure.doubleValue(NonSI.INCH);
            break;
        case Miles:
            converted = measure.doubleValue(NonSI.MILE);
            break;
        case NauticalMiles:
            converted = measure.doubleValue(NonSI.NAUTICAL_MILE);
            break;
        case Yards:
            converted = measure.doubleValue(NonSI.YARD);
            break;
        default:
            return converted;
        }

        return converted;
    }

    @SuppressWarnings("unchecked")
    public static double convertArea(Measure<Double, Area> measure, AreaUnit targetUnit) {
        double converted = measure.getValue();

        switch (targetUnit) {
        case Default:
            return converted;
        case Acre:
            Unit<Area> acre = (Unit<Area>) NonSI.MILE.divide(8.0).times(NonSI.FOOT).times(66.0);
            converted = measure.doubleValue(acre);
            break;
        case Hectare:
            converted = measure.doubleValue(NonSI.HECTARE);
            break;
        case SquareFeet:
            Unit<Area> sq_feet = (Unit<Area>) NonSI.FOOT.times(NonSI.FOOT);
            converted = measure.doubleValue(sq_feet);
            break;
        case SquareKilometers:
            Unit<Area> sq_km = (Unit<Area>) SI.KILOMETER.times(SI.KILOMETER);
            converted = measure.doubleValue(sq_km);
            break;
        case SquareMeters:
            converted = measure.doubleValue(SI.SQUARE_METRE);
            break;
        case SquareMiles:
            Unit<Area> sq_mile = (Unit<Area>) NonSI.MILE.times(NonSI.MILE);
            converted = measure.doubleValue(sq_mile);
            break;
        case SquareYards:
            Unit<Area> sq_yard = (Unit<Area>) NonSI.YARD.times(NonSI.YARD);
            converted = measure.doubleValue(sq_yard);
            break;
        default:
            return converted;
        }

        return converted;
    }

    @SuppressWarnings("unchecked")
    public static Unit<Length> getLengthUnit(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem horCRS = CRS.getHorizontalCRS(crs);
        if (horCRS instanceof GeographicCRS) {
            return SI.METER; // default
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
            return (Unit<Area>) distUnit.times(distUnit);
        }
    }
}
