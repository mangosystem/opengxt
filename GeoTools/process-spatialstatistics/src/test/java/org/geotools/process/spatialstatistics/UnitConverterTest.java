package org.geotools.process.spatialstatistics;

import javax.measure.Measure;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.geotools.process.spatialstatistics.core.UnitConverter;
import org.geotools.process.spatialstatistics.enumeration.AreaUnit;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.junit.Test;

public class UnitConverterTest extends SpatialStatisticsTestCase {

    @Test
    public void test() {
        // 1. Distance
        distance();

        // 2. Area
        area();

    }

    private void area() {
        final double value = 1000000;
        Measure<Double, Area> measure = Measure.valueOf(value, SI.SQUARE_METRE);

        double defaultValue1 = UnitConverter.convertArea(measure, AreaUnit.Default);
        assertTrue(defaultValue1 == value);

        double squareMeters = UnitConverter.convertArea(measure, AreaUnit.SquareMeters);
        assertTrue(squareMeters == value);

        double squareKm = UnitConverter.convertArea(measure, AreaUnit.SquareKilometers);
        assertTrue(squareKm == 1d);

        double acre = UnitConverter.convertArea(measure, AreaUnit.Acre);
        assertTrue(acre == 247.10538146716533);

        double hectare = UnitConverter.convertArea(measure, AreaUnit.Hectare);
        assertTrue(hectare == 100d);

        double squareFeet = UnitConverter.convertArea(measure, AreaUnit.SquareFeet);
        assertTrue(squareFeet == 1.0763910416709723E7);

        double squareMiles = UnitConverter.convertArea(measure, AreaUnit.SquareMiles);
        assertTrue(squareMiles == 0.38610215854244584);

        double squareYards = UnitConverter.convertArea(measure, AreaUnit.SquareYards);
        assertTrue(squareYards == 1195990.0463010801);
    }

    private void distance() {
        final double value = 1000;
        Measure<Double, Length> measure = Measure.valueOf(value, SI.METRE);

        double defaultValue1 = UnitConverter.convertDistance(measure, DistanceUnit.Default);
        double defaultValue2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, SI.METRE);
        assertTrue(defaultValue1 == value && defaultValue1 == defaultValue2);

        double meter1 = UnitConverter.convertDistance(measure, DistanceUnit.Meters);
        double meter2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, SI.METRE);
        assertTrue(meter1 == value && meter1 == meter2);

        double km1 = UnitConverter.convertDistance(measure, DistanceUnit.Kilometers);
        double km2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, SI.KILOMETER);
        assertTrue(km1 == 1d && km1 == km2);

        double feet1 = UnitConverter.convertDistance(measure, DistanceUnit.Feet);
        double feet2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, NonSI.FOOT);
        assertTrue(feet1 == 3280.839895013123 && feet1 == feet2);

        double inches1 = UnitConverter.convertDistance(measure, DistanceUnit.Inches);
        double inches2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, NonSI.INCH);
        assertTrue(inches1 == 39370.07874015748 && inches1 == inches2);

        double miles1 = UnitConverter.convertDistance(measure, DistanceUnit.Miles);
        double miles2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, NonSI.MILE);
        assertTrue(miles1 == 0.621371192237334 && miles1 == miles2);

        double nmiles1 = UnitConverter.convertDistance(measure, DistanceUnit.NauticalMiles);
        double nmiles2 = UnitConverter.convertDistance(value, DistanceUnit.Meters,
                NonSI.NAUTICAL_MILE);
        assertTrue(nmiles1 == 0.5399568034557235 && nmiles1 == nmiles2);

        double yards1 = UnitConverter.convertDistance(measure, DistanceUnit.Yards);
        double yards2 = UnitConverter.convertDistance(value, DistanceUnit.Meters, NonSI.YARD);
        assertTrue(yards1 == 1093.6132983377079 && yards1 == yards2);

    }

}
