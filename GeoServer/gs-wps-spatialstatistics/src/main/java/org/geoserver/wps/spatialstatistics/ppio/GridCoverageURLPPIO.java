/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.util.logging.Logging;

/**
 * A PPIO to generate grid coverage from URL.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GridCoverageURLPPIO extends CDataPPIO {
    protected static final Logger LOGGER = Logging.getLogger(GridCoverageURLPPIO.class);

    private WPSResourceManager resourceManager;

    private GeoServer geoServer;

    protected GridCoverageURLPPIO(GeoServer geoServer, WPSResourceManager resourceManager) {
        super(GridCoverage2D.class, GridCoverage2D.class, "text/plain");
        this.geoServer = geoServer;
        this.resourceManager = resourceManager;
    }

    @Override
    public Object decode(String input) throws Exception {
        WPSInfo wps = geoServer.getService(WPSInfo.class);

        URL url = new URL(input);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout((int) wps.getConnectionTimeout());
        conn.setReadTimeout((int) wps.getConnectionTimeout());

        return decode(conn.getInputStream());
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        String fileName = generateUniqeuFileName();
        File fileToSave = resourceManager.getOutputResource(null, fileName).file();

        // write the files to the disk
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileToSave);
            IOUtils.copy(input, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }

        return RasterHelper.openGridCoverage(fileToSave);
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Writer writer = new OutputStreamWriter(os);
        try {
            if (value != null) {
                GridCoverage2D coverage = (GridCoverage2D) value;

                String fileName = generateUniqeuFileName();
                File fileToSave = resourceManager.getOutputResource(null, fileName).file();

                RasterExportOperation op = new RasterExportOperation();
                op.saveAsGeoTiff(coverage, fileToSave.getAbsolutePath());

                URL url = new URL(resourceManager.getOutputResourceUrl(fileName, "image/tiff"));
                writer.write(url.toExternalForm());
            }
        } finally {
            writer.flush();
        }
    }

    @Override
    public String getFileExtension() {
        return "tif";
    }

    private String generateUniqeuFileName() {
        return UUID.randomUUID().toString() + ".tif";
    }
}