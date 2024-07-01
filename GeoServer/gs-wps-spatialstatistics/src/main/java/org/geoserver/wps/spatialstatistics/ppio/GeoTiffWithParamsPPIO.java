/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * (c) 2022 Mango System
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageWriteParam;

import org.apache.commons.io.IOUtils;
import org.geoserver.wcs.responses.GeoTiffWriterHelper;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.BinaryPPIO;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;

/**
 * Decodes/encodes a GeoTIFF file
 *
 * @author Andrea Aime - OpenGeo
 * @author Simone Giannecchini, GeoSolutions
 * @author Daniele Romagnoli, GeoSolutions
 * @author Minpa Lee, Mango System
 * 
 * @source https://github.com/geoserver/geoserver/blob/main/src/extension/wps/wps-core/src/main/java/org/geoserver/wps/ppio/GeoTiffPPIO.java
 */
public class GeoTiffWithParamsPPIO extends BinaryPPIO {
    private static final Logger LOGGER = Logging.getLogger(GeoTiffWithParamsPPIO.class);

    protected static final String TILE_WIDTH_KEY = "tilewidth";

    protected static final String TILE_HEIGHT_KEY = "tileheight";

    protected static final String COMPRESSION_KEY = "compression";

    protected static final String WRITENODATA_KEY = "writenodata";

    private static final Set<String> SUPPORTED_PARAMS = new HashSet<>();

    private static final String SUPPORTED_PARAMS_LIST;

    static {
        SUPPORTED_PARAMS.add(TILE_WIDTH_KEY);
        SUPPORTED_PARAMS.add(TILE_HEIGHT_KEY);
        SUPPORTED_PARAMS.add(COMPRESSION_KEY);
        SUPPORTED_PARAMS.add(QUALITY_KEY);
        SUPPORTED_PARAMS.add(WRITENODATA_KEY);

        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (String param : SUPPORTED_PARAMS) {
            sb.append(prefix).append(param);
            prefix = " / ";
        }
        SUPPORTED_PARAMS_LIST = sb.toString();
    }

    @SuppressWarnings("serial")
    final Map<String, Object> ENCODING_PARAMS = new HashMap<String, Object>() {
        {
            // GeoServer WCS Default = MODE_EXPLICIT, LZW, 0.75F, 256, 256
            // GDAL BLOCKXSIZE = 256, BLOCKYSIZE = 256
            put(TILE_WIDTH_KEY, String.valueOf(256));
            put(TILE_HEIGHT_KEY, String.valueOf(256));

            // COMPRESSION=LZW
            put(COMPRESSION_KEY, "LZW");
            put(QUALITY_KEY, String.valueOf(0.75));

            // NoData setting, Default is true
            put(WRITENODATA_KEY, String.valueOf(true));
        }
    };

    protected GeoTiffWithParamsPPIO() {
        // http://docs.opengeospatial.org/is/19-008r4/19-008r4.html#_media_types_for_geotiff_data_encoding
        super(GridCoverage2D.class, GridCoverage2D.class, "image/tiff; application=geotiff");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "tiff", root);
        try (FileOutputStream os = new FileOutputStream(f)) {
            IOUtils.copy(input, os);
        }

        // and then we try to read it as a geotiff
        AbstractGridFormat format = GridFormatFinder.findFormat(f);
        if (format instanceof UnknownFormat) {
            throw new WPSException(
                    "Could not find the GeoTIFF GT2 format, please check it's in the classpath");
        }
        return format.getReader(f).read(null);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        encode(value, ENCODING_PARAMS, os);
    }

    @Override
    public void encode(Object value, Map<String, Object> encodingParameters, OutputStream os)
            throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        GeoTiffWriterHelper helper = new GeoTiffWriterHelper(coverage);

        encodingParameters = encodingParameters == null ? ENCODING_PARAMS : encodingParameters;
        setEncodingParams(helper, encodingParameters);

        try {
            helper.write(os);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
    }

    private void setEncodingParams(GeoTiffWriterHelper helper,
            Map<String, Object> encodingParameters) {
        if (encodingParameters != null && !encodingParameters.isEmpty()) {
            for (String encodingParam : encodingParameters.keySet()) {
                if (!SUPPORTED_PARAMS.contains(encodingParam)) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning("The specified parameter will be ignored: " + encodingParam
                                + " Supported parameters are in the list: "
                                + SUPPORTED_PARAMS_LIST);
                    }
                }
            }

            GeoTiffWriteParams writeParams = helper.getImageIoWriteParams();
            if (writeParams != null) {
                // Inner Tiling Settings
                if (encodingParameters.containsKey(TILE_WIDTH_KEY)
                        && encodingParameters.containsKey(TILE_HEIGHT_KEY)) {
                    String tileWidth = (String) encodingParameters.get(TILE_WIDTH_KEY);
                    String tileHeight = (String) encodingParameters.get(TILE_HEIGHT_KEY);
                    try {
                        int tw = Integer.parseInt(tileWidth);
                        int th = Integer.parseInt(tileHeight);
                        writeParams.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
                        writeParams.setTiling(tw, th);
                    } catch (NumberFormatException nfe) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info("Specified tiling parameters are not valid. tileWidth = "
                                    + tileWidth + " tileHeight = " + tileHeight);
                        }
                    }
                }

                // COMPRESSION Settings
                if (encodingParameters.containsKey(COMPRESSION_KEY)) {
                    String compressionType = (String) encodingParameters.get(COMPRESSION_KEY);
                    writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParams.setCompressionType(compressionType);
                    if (encodingParameters.containsKey(QUALITY_KEY)) {
                        String compressionQuality = (String) encodingParameters.get(QUALITY_KEY);
                        try {
                            writeParams.setCompressionQuality(Float.parseFloat(compressionQuality));
                        } catch (NumberFormatException nfe) {
                            if (LOGGER.isLoggable(Level.INFO)) {
                                LOGGER.info(
                                        "Specified quality is not valid (it should be in the range [0,1])."
                                                + " compressionQuality = " + compressionQuality);
                            }
                        }
                    }
                }
            }

            ParameterValueGroup geotoolsWriteParams = helper.getGeotoolsWriteParams();
            if (geotoolsWriteParams != null && encodingParameters.containsKey(WRITENODATA_KEY)) {
                geotoolsWriteParams.parameter(GeoTiffFormat.WRITE_NODATA.getName().toString())
                        .setValue(Boolean
                                .parseBoolean((String) encodingParameters.get(WRITENODATA_KEY)));
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "tif";
    }
}
