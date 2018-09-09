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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.Envelope;

/**
 * Raster Environment
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterEnvironment {
    protected static final Logger LOGGER = Logging.getLogger(RasterEnvironment.class);

    private double cellSizeX = Double.NaN;

    private double cellSizeY = Double.NaN;

    private ReferencedEnvelope extent = new ReferencedEnvelope();

    private GridCoverage2D maskDataset = null;

    private String prefix = "GT";

    private String workspace = null;

    public double getCellSizeX() {
        if (cellSizeX == Double.NaN || cellSizeX <= 0) {
            return Math.min(extent.getWidth(), extent.getHeight()) / 250.0;
        }
        return cellSizeX;
    }

    public void setCellSizeX(double cellSizeX) {
        this.cellSizeX = cellSizeX;
    }

    public double getCellSizeY() {
        if (cellSizeY == Double.NaN || cellSizeY <= 0) {
            return Math.min(extent.getWidth(), extent.getHeight()) / 250.0;
        }
        return cellSizeY;
    }

    public void setCellSizeY(double cellSizeY) {
        this.cellSizeY = cellSizeY;
    }

    public ReferencedEnvelope getExtent() {
        return extent;
    }

    public void setExtent(Envelope extent) {
        this.extent = new ReferencedEnvelope(extent);
    }

    public void setExtent(ReferencedEnvelope extent) {
        this.extent = extent;
    }

    public GridCoverage2D getMaskDataset() {
        return maskDataset;
    }

    public void setMaskDataset(GridCoverage2D maskDataset) {
        this.maskDataset = maskDataset;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null && prefix.length() > 0) {
            this.prefix = prefix;
        }
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspaceDir) {
        if (workspaceDir != null && workspaceDir.length() > 0) {
            File dirFile = new File(workspaceDir);
            if (dirFile.isDirectory()) {
                this.workspace = workspaceDir;
            } else if (!dirFile.exists()) {
                if (dirFile.mkdir()) {
                    this.workspace = workspaceDir;
                } else {
                    System.out.println("Error occured! cannot create " + workspaceDir
                            + " Directory");
                }
            }
        }
    }

    public String getUniqueName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss_S");
        String uid = sdf.format(Calendar.getInstance().getTime());

        // TODO UUID Utils
        String uniqueName = workspace + "_" + uid + ".tif";

        return uniqueName;
    }
}
