package org.geotools.process.spatialstatistics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.gridcoverage.RasterFunctionalSurface;
import org.geotools.process.spatialstatistics.storage.DataStoreFactory;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LineOfSightProcessTest2 extends SpatialStatisticsTestCase {

    @Test
    public void test() throws Exception {
        File file = new File("E:\\Temp\\Military-Data\\LOS\\DEM10.tif");
        String outputFolder = "E:\\\\Temp\\\\Military-Data\\\\LOS";

        // open coverage
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        GridCoverage2DReader reader = format.getReader(file);
        GridCoverage2D inputCoverage = reader.read(null);
        assertNotNull(inputCoverage);

        // build parameters
        Geometry observerPoint = null;
        Geometry targetPoint = null;
        String testCase = "1";
        double observerOffset = 2.0d;

        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

        // case 1
        observerPoint = gf.createPoint(new Coordinate(269751, 549201));
        targetPoint = gf.createPoint(new Coordinate(269751, 546943));
        testCase = "1";

        // case 2
        observerPoint = gf.createPoint(new Coordinate(273416, 546794));
        targetPoint = gf.createPoint(new Coordinate(268844, 549825));
        testCase = "2";

        // case 3
        // observerPoint = gf.createPoint(new Coordinate(270662, 547515));
        // targetPoint = gf.createPoint(new Coordinate(273622, 547515));
        // testCase = "3";

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterLinearLOSProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterLinearLOSProcessFactory.observerPoint.key, observerPoint);
        map.put(RasterLinearLOSProcessFactory.observerOffset.key, observerOffset);
        map.put(RasterLinearLOSProcessFactory.targetPoint.key, targetPoint);
        map.put(RasterLinearLOSProcessFactory.useCurvature.key, Boolean.TRUE);

        // execute process
        SimpleFeatureCollection losFeatures = null;

        org.geotools.process.Process losProcess = new RasterLinearLOSProcess(null);
        Map<String, Object> resultMap = losProcess.execute(map, null);
        losFeatures = (SimpleFeatureCollection) resultMap
                .get(RasterLinearLOSProcessFactory.RESULT.key);
        assertTrue(losFeatures.size() > 0);

        exportToShapefile(losFeatures, outputFolder, "GXT-LineOfSight" + testCase);
        calculate_length(losFeatures);

        // Point Test
        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        LineSegment segment = new LineSegment(observerPoint.getCoordinate(),
                targetPoint.getCoordinate());
        LineString userLine = segment.toGeometry(observerPoint.getFactory());

        RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
        LineString los = process.getLineOfSight(userLine, observerOffset, true, true, 0.13d);

        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("LOS-POINTS", Point.class, crs);
        featureType = FeatureTypes.add(featureType, "Visible", Integer.class, 5);

        // prepare transactional feature store
        ListFeatureCollection pointFc = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        Coordinate[] coordinates = los.getCoordinates();
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coordinate = coordinates[i];
            int visible = (int) coordinate.z;

            String fid = featureType.getTypeName() + "." + (i + 1);
            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(observerPoint.getFactory().createPoint(coordinate));
            newFeature.setAttribute("Visible", visible);
            pointFc.add(newFeature);
        }

        exportToShapefile(pointFc, outputFolder, "GXT-POINTS" + testCase);

        System.out.println("완료: Test Case " + testCase);

    }

    SimpleFeatureCollection exportToShapefile(SimpleFeatureCollection inputFeatures,
            String outputFolder, String name) throws IOException {
        ShapeExportOperation operation = new ShapeExportOperation();
        operation.setOutputDataStore(DataStoreFactory.getShapefileDataStore(outputFolder));
        operation.setOutputTypeName(name);

        return operation.execute(inputFeatures).getFeatures();
    }

    void calculate_length(SimpleFeatureCollection inputFeatures) {
        double vis = 0.0d;
        double inv = 0.0d;

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                int visible = Converters.convert(feature.getAttribute("Visible"), Integer.class);
                if (visible == 1) {
                    vis += geometry.getLength();
                } else {
                    inv += geometry.getLength();
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            featureIter.close();
        }

        System.out.println("Visible: " + vis);
        System.out.println("InVisible: " + inv);
    }

}
