/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.common;

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
 * @author Minpa Lee
 * @since 1.0
 * @version $Id: RasterSaveAsOp.java 1 2011-09-01 11:22:29Z minpa.lee $
 */
@SuppressWarnings("nls")
public class RasterSaveAsOp {
    protected static final Logger LOGGER = Logging.getLogger(RasterSaveAsOp.class);

    // http://www.javadocexamples.com/java_source/org/geotools/data/gtopo30/GTopo30DataSource.java.html
    // http://www.mail-archive.com/geotools-gt2-users@lists.sourceforge.net/msg09158.html

    // CompressionType : CCITT RLE, CCITT T.4, CCITT T.6, LZW, JPEG, ZLib, 
    //                 PackBits, Deflate, EXIF, JPEG

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

    public GridCoverage2D saveAsGeoTiff(GridCoverage2D sourceCoverage, String tiffFile) {
        if (!confirmRasterFile(tiffFile)) {
            return null;
        }

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
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } finally {
            writer.dispose();
        }

        return null;
    }

    private boolean confirmRasterFile(String tiffFile) {
        int pos = tiffFile.lastIndexOf('.');
        String fileNoExt = tiffFile.substring(0, pos);

        File outputFile = null;
        String[] extList = new String[] { ".tfw", ".aux", ".rrd", ".xml", ".vat.dbf" };
        for (String ext : extList) {
            outputFile = new File(fileNoExt + ext);
            if (outputFile.exists() && outputFile.isFile()) {
                outputFile.delete();
            }
        }

        // PointToRasterOp.tif.vat.dbf
        outputFile = new File(tiffFile + ".vat.dbf");
        if (outputFile.exists() && outputFile.isFile()) {
            outputFile.delete();
        }

        // finally delete tif file
        outputFile = new File(tiffFile);
        if (outputFile.exists() && outputFile.isFile()) {
            return outputFile.delete();
        }

        return true;
    }
}
