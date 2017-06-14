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
package org.geotools.process.spatialstatistics;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.MapToImageParam;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * generate map image from raster and map parameters.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterToImageProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterToImageProcess.class);

    public RasterToImageProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static MapToImageParam process(GridCoverage2D coverage, ReferencedEnvelope bbox,
            CoordinateReferenceSystem crs, Style style, Integer width, Integer height,
            String format, Boolean transparent, String bgColor, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterToImageProcessFactory.coverage.key, coverage);
        map.put(RasterToImageProcessFactory.bbox.key, bbox);
        map.put(RasterToImageProcessFactory.crs.key, crs);
        map.put(RasterToImageProcessFactory.style.key, style);
        map.put(RasterToImageProcessFactory.width.key, width);
        map.put(RasterToImageProcessFactory.height.key, height);
        map.put(RasterToImageProcessFactory.format.key, format);
        map.put(RasterToImageProcessFactory.transparent.key, transparent);
        map.put(RasterToImageProcessFactory.bgColor.key, bgColor);

        Process process = new RasterToImageProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (MapToImageParam) resultMap.get(RasterToImageProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D coverage = (GridCoverage2D) Params.getValue(input,
                RasterToImageProcessFactory.coverage, null);
        String bbox = (String) Params.getValue(input, RasterToImageProcessFactory.bbox, null);
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) Params.getValue(input,
                RasterToImageProcessFactory.crs, null);
        Style style = (Style) Params.getValue(input, RasterToImageProcessFactory.style, null);
        Integer width = (Integer) Params.getValue(input, RasterToImageProcessFactory.width, null);
        Integer height = (Integer) Params.getValue(input, RasterToImageProcessFactory.height, null);
        String format = (String) Params.getValue(input, RasterToImageProcessFactory.format,
                RasterToImageProcessFactory.format.sample);
        Boolean transparent = (Boolean) Params.getValue(input,
                RasterToImageProcessFactory.transparent,
                RasterToImageProcessFactory.transparent.sample);
        String bgColor = (String) Params.getValue(input, RasterToImageProcessFactory.bgColor,
                RasterToImageProcessFactory.bgColor.sample);

        if (coverage == null || width == null || height == null) {
            throw new NullPointerException("coverage, width, height parameters required");
        }

        if (width <= 0 || height <= 0) {
            throw new ProcessException("width, height parameters mustbe grater than 0!");
        }

        // start process
        if (crs == null) {
            crs = coverage.getCoordinateReferenceSystem();
        }

        if (style == null) {
            SSStyleBuilder sb = new SSStyleBuilder(null);
            style = sb.getDefaultGridCoverageStyle(coverage);
        }

        ReferencedEnvelope mapExtent = this.getBoundingBox(bbox, crs);
        if (mapExtent == null || mapExtent.isEmpty()) {
            mapExtent = new ReferencedEnvelope(coverage.getEnvelope());
        }

        Color backgroundColor = Color.WHITE;
        try {
            backgroundColor = Color.decode(bgColor);
        } catch (NumberFormatException ne) {
            LOGGER.warning("Color Decode Error: " + ne.getMessage());
        }

        // start process
        MapToImageParam mapImage = new MapToImageParam();
        mapImage.setInputCoverage(coverage);
        mapImage.setStyle(style);
        mapImage.setBackgroundColor(backgroundColor);
        mapImage.setWidth(width);
        mapImage.setHeight(height);
        mapImage.setMapExtent(mapExtent);
        mapImage.setSrs(crs);
        mapImage.setFormat(format);
        mapImage.setTransparent(transparent);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterToImageProcessFactory.RESULT.key, mapImage);
        return resultMap;
    }

    private ReferencedEnvelope getBoundingBox(String bBox, CoordinateReferenceSystem crs) {
        if (bBox == null || bBox.isEmpty()) {
            return new ReferencedEnvelope();
        }

        try {
            // BBOX: XMIN, YMIN, XMAX, YMAX
            String[] coords = bBox.split(",");
            double minx = Double.parseDouble(coords[0]);
            double miny = Double.parseDouble(coords[1]);
            double maxx = Double.parseDouble(coords[2]);
            double maxy = Double.parseDouble(coords[3]);

            return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new ReferencedEnvelope(crs);
    }
}
