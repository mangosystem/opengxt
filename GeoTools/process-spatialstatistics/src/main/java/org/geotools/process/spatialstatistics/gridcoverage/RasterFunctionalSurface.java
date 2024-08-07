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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.SlopeType;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.linemerge.LineMerger;

/**
 * Quantify and visualize a terrain landform represented by a digital elevation model.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterFunctionalSurface {
    protected static final Logger LOGGER = Logging.getLogger(RasterFunctionalSurface.class);

    public enum RasterFunctionType {
        Elevation, SlopeDegrees, SlopePercent, SlopeRadians, AspectDegrees, AspectRadians
    }

    public static final int VISIBLE = 1;

    public static final int INVISIBLE = 0;

    private GridCoverage2D grid2D;

    private PlanarImage image;

    private java.awt.Rectangle bounds;

    private Double noData = Double.NaN;

    private double cellSizeX = 0;

    private double cellSizeY = 0;

    private double _8DX = cellSizeX * 8;

    private double _8DY = cellSizeY * 8;

    private boolean isGeographic = false;

    private GeodeticCalculator calculator = null;

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public RasterFunctionalSurface(GridCoverage2D srcCoverage) {
        this.grid2D = srcCoverage;

        GridGeometry2D gridGeometry2D = srcCoverage.getGridGeometry();
        AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

        this.cellSizeX = Math.abs(gridToWorld.getScaleX());
        this.cellSizeY = Math.abs(gridToWorld.getScaleY());
        this._8DX = cellSizeX * 8;
        this._8DY = cellSizeY * 8;

        this.noData = RasterHelper.getNoDataValue(srcCoverage);

        this.image = (PlanarImage) srcCoverage.getRenderedImage();
        this.bounds = image.getBounds();

        CoordinateReferenceSystem crs = srcCoverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem hor = CRS.getHorizontalCRS(crs);
        this.isGeographic = hor instanceof GeographicCRS;
        if (this.isGeographic) {
            this.calculator = new GeodeticCalculator(crs);
        }
    }

    public Map<Integer, Geometry> mergeLineOfSight(LineString lineString) {
        Map<Integer, Geometry> map = new HashMap<Integer, Geometry>();

        LineMerger vm = new LineMerger();
        LineMerger ivm = new LineMerger();

        GeometryFactory gf = lineString.getFactory();

        Coordinate[] coordinates = lineString.getCoordinates();
        for (int i = 1; i < coordinates.length; i++) {
            int visible = (int) coordinates[i].z;
            LineSegment lineSeg = new LineSegment(coordinates[i - 1], coordinates[i]);
            LineString line = lineSeg.toGeometry(gf);

            if (visible == 1) {
                vm.add(line);
            } else {
                ivm.add(line);
            }
        }

        @SuppressWarnings("rawtypes")
        Collection lineStrings = vm.getMergedLineStrings();
        if (lineStrings.size() > 0) {
            Geometry lines = gf.createMultiLineString(simplify(lineStrings));
            map.put(Integer.valueOf(1), lines);
        }

        lineStrings = ivm.getMergedLineStrings();
        if (lineStrings.size() > 0) {
            Geometry lines = gf.createMultiLineString(simplify(lineStrings));
            map.put(Integer.valueOf(0), lines);
        }

        return map;
    }

    @SuppressWarnings("rawtypes")
    private LineString[] simplify(Collection lineStrings) {
        List<LineString> lineList = new ArrayList<LineString>();

        Iterator it = lineStrings.iterator();
        while (it.hasNext()) {
            LineString lineString = (LineString) it.next();

            Coordinate[] coords = new Coordinate[2];
            coords[0] = lineString.getStartPoint().getCoordinate();
            coords[1] = lineString.getEndPoint().getCoordinate();
            lineList.add(lineString.getFactory().createLineString(coords));
        }

        return GeometryFactory.toLineStringArray(lineList);
    }

    public LineString getLineOfSight(Coordinate observer, Coordinate target, double observerOffset,
            boolean useCurvature) {
        return getLineOfSight(gf.createLineString(new Coordinate[] { observer, target }),
                observerOffset, useCurvature);
    }

    public LineString getLineOfSight(Coordinate observer, Coordinate target, double observerOffset,
            boolean useCurvature, boolean useRefraction, double refractionFactor) {
        return getLineOfSight(gf.createLineString(new Coordinate[] { observer, target }),
                observerOffset, useCurvature, useRefraction, refractionFactor);
    }

    public LineString getLineOfSight(LineString segment, double observerOffset,
            boolean useCurvature) {
        return getLineOfSight(segment, observerOffset, useCurvature, false, 0.13);
    }

    public LineString getLineOfSight(LineString segment, double observerOffset,
            boolean useCurvature, boolean useRefraction, double refractionFactor) {
        final LinkedList<Coordinate> ros = new LinkedList<Coordinate>();

        Coordinate from = segment.getStartPoint().getCoordinate();
        Coordinate to = segment.getEndPoint().getCoordinate();

        if (isGeographic) {
            calculator.setStartingGeographicPoint(from.x, from.y);
        }

        // Source point always sees itself
        from.z = this.getElevation(from);

        if (SSUtils.compareDouble(from.z, noData)) {
            ros.add(new Coordinate(from.x, from.y, INVISIBLE));
            ros.add(new Coordinate(to.x, to.y, INVISIBLE));
            return segment.getFactory().createLineString(CoordinateArrays.toCoordinateArray(ros));
        }

        // The offset is the vertical distance (in surface units) to be added to the z-value of a
        // location on the surface.
        from.z += observerOffset;
        ros.add(new Coordinate(from.x, from.y, VISIBLE));

        double segAngle = Math.atan2(to.y - from.y, to.x - from.x);
        double cosAngle = Math.cos(segAngle);
        double sinAngle = Math.sin(segAngle);
        double sumOfDistance = from.distance(to);

        int xCellCount = (int) ((to.x - from.x) / cellSizeX);
        int yCellCount = (int) ((to.y - from.y) / cellSizeY);
        int maxCellCount = Math.max(Math.abs(xCellCount), Math.abs(yCellCount));
        if (maxCellCount == 0) {
            ros.add(new Coordinate(to.x, to.y, VISIBLE));
            return segment.getFactory().createLineString(CoordinateArrays.toCoordinateArray(ros));
        }

        double cellDistance = sumOfDistance / (double) maxCellCount;

        double maxSlope = Double.MAX_VALUE;
        double distance = 0.0;

        Coordinate current = new Coordinate();
        while (distance < sumOfDistance) {
            distance += cellDistance;

            current.x = from.x + distance * cosAngle;
            current.y = from.y + distance * sinAngle;
            current.z = this.getElevation(current);

            // Skip no data points
            if (SSUtils.compareDouble(current.z, noData)) {
                continue;
            }

            if (useCurvature) {
                // Curvature and atmospheric refraction corrections
                // Z = Z0 + D^2(R - 1) ÷ d
                // where:
                // Z—corrected elevation after factoring the impact of atmospheric refraction.
                // Z0—surface elevation of the observed location.
                // D—planimetric distance between the observation feature and the observed location.
                // d—diameter of the earth, which is 12,740,000 meters.
                // R—refractivity coefficient of light. The default value of 0.13 is considered
                // appropriate under standard atmospheric pressure for daytime conditions with a
                // clear
                // sky for locations whose elevation varies between 40 and 100 meters

                final double D = from.distance(current);
                if (useRefraction) {
                    current.z = current.z + (D * D) * (refractionFactor - 1) / 12740000.0;
                } else {
                    current.z = current.z + (D * D) * (0.13 - 1) / 12740000.0;
                }
            }

            // Slope between source and current point
            // (current.z - from.z) / distance;
            final double slope = Math.atan2(current.z - from.z, distance);

            // First point is always visible to source. It's slope is the maximum slope for the
            // source to see other cells
            if (maxSlope == Double.MAX_VALUE) {
                maxSlope = slope;
                ros.add(new Coordinate(current.x, current.y, VISIBLE));
            } else if (slope <= maxSlope) {
                // If slope below maximum visible slope than cell is hidden
                ros.add(new Coordinate(current.x, current.y, INVISIBLE));
            } else {
                // If slope is above maximum visible slope than cell is visible and its slope is now
                // the maximum visible slope
                maxSlope = slope;
                ros.add(new Coordinate(current.x, current.y, VISIBLE));
            }
        }

        if (ros.size() < 2) {
            ros.add(new Coordinate(to.x, to.y, INVISIBLE));
            return segment.getFactory().createLineString(CoordinateArrays.toCoordinateArray(ros));
        }

        Coordinate[] coordinates = CoordinateArrays.toCoordinateArray(ros);
        LineString result = segment.getFactory().createLineString(coordinates);

        final double diff = result.getLength() - segment.getLength();
        if (diff > (cellDistance / 2.0)) {
            Coordinate[] coords = Arrays.copyOf(coordinates, coordinates.length - 1);
            result = segment.getFactory().createLineString(coords);
        }
        return result;
    }

    public double getSlope(Point position, SlopeType slopeType) {
        double retVal = noData;

        GridGeometry2D gg2D = grid2D.getGridGeometry();
        CoordinateReferenceSystem crs = grid2D.getCoordinateReferenceSystem();
        Coordinate coord = position.getCoordinate();
        Position gdPos = new Position2D(crs, coord.x, coord.y);

        try {
            GridCoordinates2D pos = gg2D.worldToGrid(gdPos);
            retVal = this.getSlope(pos);

            if (slopeType == SlopeType.Degree) {
                retVal = Math.toDegrees(retVal);
            } else {
                retVal = Math.tan(retVal) * 100;
            }
        } catch (InvalidGridGeometryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (TransformException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return retVal;
    }

    public double getAspect(Point position) {
        double retVal = noData;

        GridGeometry2D gg2D = grid2D.getGridGeometry();
        CoordinateReferenceSystem crs = grid2D.getCoordinateReferenceSystem();
        Coordinate coord = position.getCoordinate();
        Position gdPos = new Position2D(crs, coord.x, coord.y);

        try {
            GridCoordinates2D pos = gg2D.worldToGrid(gdPos);

            retVal = this.getAspect(pos);
        } catch (InvalidGridGeometryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (TransformException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return retVal;
    }

    public double getElevation(Point position) {
        return getElevation(position.getCoordinate());
    }

    public double getElevation(Coordinate coord) {
        double retVal = noData;

        double[] gridVals = new double[grid2D.getNumSampleDimensions()];
        CoordinateReferenceSystem crs = grid2D.getCoordinateReferenceSystem();
        Position gdPos = new Position2D(crs, coord.x, coord.y);
        try {
            grid2D.evaluate(gdPos, gridVals);
            retVal = gridVals[0];
        } catch (Exception e) {
            retVal = noData;
        }

        return retVal;
    }

    public Geometry getProfile(Geometry userLine, Double distanceTolerance) {
        Geometry profileLine = userLine;

        // densify geometry
        if (distanceTolerance != null && userLine.getLength() > distanceTolerance) {
            profileLine = Densifier.densify(userLine, distanceTolerance);
        }

        // important ! Default PositionFactory = EPSG:4326
        Coordinate[] coords = profileLine.getCoordinates();
        CoordinateReferenceSystem crs = grid2D.getCoordinateReferenceSystem();

        // interpolate points
        double[] gridVals = new double[grid2D.getNumSampleDimensions()];
        for (Coordinate coord : coords) {
            Position gdPos = new Position2D(crs, coord.x, coord.y);
            try {
                grid2D.evaluate(gdPos, gridVals);
                coord.z = gridVals[0];
            } catch (Exception e) {
                coord.z = noData;
            }
        }

        return profileLine;
    }

    private double getAspect(GridCoordinates2D pos) {

        // http://webhelp.esri.com/arcgisdesktop/9.2/index.cfm?TopicName=How%20Aspect%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.
        // [dz/dx] = ((c + 2f + i) - (a + 2d + g)) / 8
        // [dz/dy] = ((g + 2h + i) - (a + 2b + c)) / 8
        // aspect = 57.29578 * atan2([dz/dy], -[dz/dx])

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3);
        if (noData == mx[1][1]) {
            return noData;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DX);
        double rise_run = (dZdX * dZdX) + (dZdY * dZdY);
        double slope = Math.toDegrees(Math.atan(Math.sqrt(rise_run)));
        if (Double.isNaN(slope) || slope == 0) {
            return -1;
        }

        // aspect
        dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (8.0);
        dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (8.0);

        double aspect = (180.0 / Math.PI) * Math.atan2(dZdY, -dZdX);

        if (aspect < 0) {
            aspect = 90.0 - aspect;
        } else if (aspect > 90.0) {
            aspect = 360.0 - aspect + 90.0;
        } else {
            aspect = 90.0 - aspect;
        }

        return aspect;
    }

    @SuppressWarnings("unused")
    private double getHillShade(GridCoordinates2D pos, final double azimuth, final double altitude,
            final double zFactor) {
        // http://webhelp.esri.com/arcgisdesktop/9.2/index.cfm?TopicName=How%20Hillshade%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.
        // Hillshade = 255.0 * ( ( cos(Zenith_rad) * cos(Slope_rad) ) + ( sin(Zenith_rad) *
        // sin(Slope_rad) * cos(Azimuth_rad - Aspect_rad) ) )

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3);
        if (noData == mx[1][1]) {
            return noData;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DY);
        if (Double.isNaN(dZdX) || Double.isNaN(dZdY)) {
            return noData;
        }

        double zenith_deg = 90 - altitude;
        double zenith_rad = zenith_deg * (Math.PI / 180.0);

        double azimuth_math = 360.0 - azimuth + 90;
        if (azimuth_math > 360) {
            azimuth_math = azimuth_math - 360.0;
        }

        double azimuth_rad = azimuth_math * (Math.PI / 180.0);
        double slope_rad = Math.atan(Math.sqrt(dZdX * dZdX + dZdY * dZdY) * zFactor);

        double aspect_rad = 0.0;
        if (dZdX == 0.0) {
            if (dZdY > 0) {
                aspect_rad = Math.PI / 2.0;
            } else if (dZdY < 0) {
                aspect_rad = (2.0 * Math.PI) - (Math.PI / 2.0);
            } else {
                aspect_rad = Math.atan2(dZdY, -dZdX);
                if (aspect_rad < 0) {
                    aspect_rad = (2.0 * Math.PI) + aspect_rad;
                }
            }
        } else {
            aspect_rad = Math.atan2(dZdY, -dZdX);
            if (aspect_rad < 0) {
                aspect_rad = (2.0 * Math.PI) + aspect_rad;
            }
        }

        double hsdVal = 255.0
                * ((Math.cos(zenith_rad) * Math.cos(slope_rad)) + (Math.sin(zenith_rad)
                        * Math.sin(slope_rad) * Math.cos(azimuth_rad - aspect_rad)));

        return hsdVal;
    }

    private double getSlope(GridCoordinates2D pos) {
        // http://webhelp.esri.com/arcgisdesktop/9.2/index.cfm?TopicName=How%20Slope%20works
        // Burrough, P. A. and McDonell, R.A., 1998. Principles of Geographical Information Systems
        // (Oxford University Press, New York), p. 190.
        // [dz/dx] = ((c + 2f + i) - (a + 2d + g) / (8 * x_cell_size)
        // [dz/dy] = ((g + 2h + i) - (a + 2b + c)) / (8 * y_cell_size)
        // slope_degrees = ATAN ( SQRT ( [dz/dx]2 + [dz/dy]2 ) ) * 57.29578

        // +-------+ +-------+
        // | 0 1 2 | | a b c |
        // | 3 4 5 |>| d e f |
        // | 6 7 8 | | g h i |
        // +-------+ +-------+
        double[][] mx = getSubMatrix(pos, 3, 3);
        if (noData == mx[1][1]) {
            return noData;
        }

        double dZdX = ((mx[2][0] + 2 * mx[2][1] + mx[2][2]) - (mx[0][0] + 2 * mx[0][1] + mx[0][2]))
                / (_8DX);
        double dZdY = ((mx[0][2] + 2 * mx[1][2] + mx[2][2]) - (mx[0][0] + 2 * mx[1][0] + mx[2][0]))
                / (_8DY);
        double rise_run = (dZdX * dZdX) + (dZdY * dZdY);
        if (Double.isNaN(rise_run)) {
            return noData;
        }

        return Math.atan(Math.sqrt(rise_run));
    }

    private double[][] getSubMatrix(GridCoordinates2D pos, int width, int height) {
        return getSubMatrix(pos, width, height, 1.0);
    }

    private double[][] getSubMatrix(GridCoordinates2D pos, int width, int height, double zFactor) {
        int posX = width / 2;
        int posY = height / 2;

        // upper-left corner
        GridCoordinates2D ulPos = new GridCoordinates2D(pos.x - posX, pos.y - posY);
        Rectangle rect = new Rectangle(ulPos.x, ulPos.y, width, height);

        Raster subsetRs = image.getData(rect);

        int maxCol = bounds.x + image.getWidth();
        int maxRow = bounds.y + image.getHeight();

        boolean hasNAN = false;
        double[][] mx = new double[width][height];
        for (int dy = ulPos.y, row = 0; row < subsetRs.getHeight(); dy++, row++) {
            for (int dx = ulPos.x, col = 0; col < subsetRs.getWidth(); dx++, col++) {
                if (dx < bounds.x || dy < bounds.y || dx >= maxCol || dy >= maxRow) {
                    mx[col][row] = Double.NaN;
                    hasNAN = true;
                } else {
                    mx[col][row] = subsetRs.getSampleDouble(dx, dy, 0) * zFactor;
                }
            }
        }

        // http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#/How_Slope_works/009z000000vz000000/
        // If any neighborhood cells are NoData, they are assigned the value of the center cell;
        // then the slope is computed.
        if (hasNAN) {
            for (int dcol = 0; dcol < width; dcol++) {
                for (int drow = 0; drow < height; drow++) {
                    if (Double.isNaN(mx[dcol][drow])) {
                        mx[dcol][drow] = mx[1][1];
                    }
                }
            }
        }

        return mx;
    }

}
