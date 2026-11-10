package org.geotools.process.spatialstatistics;

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

final class TerrainTestSupport {
    static GridCoverage2D dem(double[][] heights) {
        return dem(heights, new AffineTransform(1, 0, 0, 1, .5, .5),
                DefaultEngineeringCRS.CARTESIAN_2D, -9999d);
    }

    static GridCoverage2D dem(double[][] heights, AffineTransform centres,
            CoordinateReferenceSystem crs, Number noData) {
        return dem(heights, centres, crs, noData, 0, 0);
    }

    static GridCoverage2D dem(double[][] heights, AffineTransform centres,
            CoordinateReferenceSystem crs, Number noData, int minX, int minY) {
        int h = heights.length, w = heights[0].length;
        java.awt.image.SampleModel model = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, 16, 16, 1);
        TiledImage image = new TiledImage(minX, minY, w, h, minX, minY, model,
                PlanarImage.createColorModel(model));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setSample(x + minX, y + minY, 0, heights[y][x]);
            }
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        if (noData != null) {
            properties.put("GC_NODATA", noData);
        }
        return CoverageFactoryFinder.getGridCoverageFactory(null).create("DEM", image, crs,
                new AffineTransform2D(centres), new GridSampleDimension[] {
                        new GridSampleDimension("Elevation") }, null, properties);
    }
}
