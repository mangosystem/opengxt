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
package org.geotools.process.spatialstatistics.storage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageWriteParam;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Converts a raster dataset to a raster dataset format.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterExportOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterExportOperation.class);

    // http://www.javadocexamples.com/java_source/org/geotools/data/gtopo30/GTopo30DataSource.java.html
    // http://www.mail-archive.com/geotools-gt2-users@lists.sourceforge.net/msg09158.html

    // CompressionType : CCITT RLE, CCITT T.4, CCITT T.6, LZW, JPEG, ZLib,
    // PackBits, Deflate, EXIF, JPEG

    // GeoServer WCS Default = MODE_EXPLICIT, LZW, 0.75F, 256, 256

    private String compressionType = "LZW"; // default //$NON-NLS-1$

    private Float compressionQuality = 1.0f; // default

    private int tileWidth = 128; // default

    private int tileHeight = 128; // default

    private boolean useTileMode = true; // default

    private boolean useCompressionMode = false; // default

    public boolean isCompressionMode() {
        return useCompressionMode;
    }

    public void setCompressionMode(boolean useCompressionMode) {
        this.useCompressionMode = useCompressionMode;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public Float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(Float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHEight(int tileHEight) {
        this.tileHeight = tileHEight;
    }

    public boolean isTileMode() {
        return useTileMode;
    }

    public void setTileMode(boolean useTileMode) {
        this.useTileMode = useTileMode;
    }

    public GridCoverage2D saveAsGeoTiff(GridCoverage2D sourceCoverage, String tiffFile)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        // getting the write parameters
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        if (useCompressionMode) {
            wp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            wp.setCompressionType(compressionType);
            wp.setCompressionQuality(compressionQuality);
        } else {
            wp.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
        }

        if (useTileMode) {
            wp.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
            wp.setTiling(tileWidth, tileHeight);
        } else {
            wp.setTilingMode(ImageWriteParam.MODE_DEFAULT);
        }

        GeneralParameterValue[] paramValues = null;
        final GeoTiffFormat format = new GeoTiffFormat();

        CoordinateReferenceSystem crs = sourceCoverage.getCoordinateReferenceSystem();
        if (crs == null) {
            LOGGER.log(Level.WARNING, "GeoTools does not support null CoordinateReferenceSystem!");

            final ParameterValue<Boolean> tfw = GeoTiffFormat.WRITE_TFW.createValue();
            tfw.setValue(true);

            paramValues = new GeneralParameterValue[] { tfw };
        } else {
            // setting the write parameters for this geotiff
            final ParameterValueGroup pvg = format.getWriteParameters();
            final String paramKey = AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString();
            pvg.parameter(paramKey).setValue(wp);

            paramValues = pvg.values().toArray(new GeneralParameterValue[1]);
        }

        File outputFile = new File(tiffFile);
        GeoTiffWriter writer = (GeoTiffWriter) format.getWriter(outputFile);
        try {
            writer.write(sourceCoverage, paramValues);

            // open GridCoverage2D
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            GeoTiffReader reader = new GeoTiffReader(outputFile, hints);
            return reader.read(null);
        } finally {
            writer.dispose();
        }
    }
}
