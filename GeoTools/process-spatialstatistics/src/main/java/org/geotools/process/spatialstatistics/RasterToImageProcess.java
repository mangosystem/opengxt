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

import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.MapToImageParam;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.DoubleStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitor.StatisticsStrategy;
import org.geotools.process.spatialstatistics.core.StatisticsVisitorResult;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;
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

    private StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

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
            style = getDefaultStyle(coverage);
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

    private Style getDefaultStyle(GridCoverage2D coverage) {
        Style rasterStyle = null;
        int numBands = coverage.getNumSampleDimensions();

        if (numBands >= 3) {
            rasterStyle = createRGBStyle(coverage);
        } else {
            Color[] colors = new Color[] { new Color(0, 0, 0, 0), Color.BLUE, Color.CYAN,
                    Color.GREEN, Color.YELLOW, Color.RED };

            StatisticsStrategy strategy = new DoubleStrategy();
            strategy.setNoData(RasterHelper.getNoDataValue(coverage));

            StatisticsVisitor visitor = new StatisticsVisitor(strategy);
            visitor.visit(coverage, 0);
            StatisticsVisitorResult ret = visitor.getResult();

            String[] descs = new String[] { "No Data", "LL", "LM", "M", "MH", "HH" };

            double mean = ret.getMean();
            double nodata = Double.parseDouble(ret.getNoData().toString());
            double[] values = new double[] { nodata, ret.getMinimum(),
                    (ret.getMinimum() + mean) / 2.0, mean, (ret.getMaximum() + mean) / 2.0,
                    ret.getMaximum() };

            StyleBuilder sb = new StyleBuilder();
            ColorMap colorMap = sb.createColorMap(descs, values, colors, ColorMap.TYPE_RAMP);
            RasterSymbolizer rsDem = sb.createRasterSymbolizer(colorMap, 1);
            rasterStyle = sb.createStyle(rsDem);
        }

        return rasterStyle;
    }

    private Style createRGBStyle(GridCoverage2D srcGc) {
        // We need at least three bands to create an RGB style
        int numBands = srcGc.getNumSampleDimensions();
        if (numBands < 3) {
            return null;
        }
        // Get the names of the bands
        String[] sampleDimensionNames = new String[numBands];
        for (int i = 0; i < numBands; i++) {
            GridSampleDimension dim = srcGc.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }

        final int RED = 0, GREEN = 1, BLUE = 2;
        int[] channelNum = { -1, -1, -1 };
        // We examine the band names looking for "red...", "green...", "blue...".
        // Note that the channel numbers we record are indexed from 1, not 0.
        for (int i = 0; i < numBands; i++) {
            String name = sampleDimensionNames[i].toLowerCase();
            if (name != null) {
                if (name.matches("red.*")) {
                    channelNum[RED] = i + 1;
                } else if (name.matches("green.*")) {
                    channelNum[GREEN] = i + 1;
                } else if (name.matches("blue.*")) {
                    channelNum[BLUE] = i + 1;
                }
            }
        }
        // If we didn't find named bands "red...", "green...", "blue..."
        // we fall back to using the first three bands in order
        if (channelNum[RED] < 0 || channelNum[GREEN] < 0 || channelNum[BLUE] < 0) {
            channelNum[RED] = 1;
            channelNum[GREEN] = 2;
            channelNum[BLUE] = 3;
        }
        // Now we create a RasterSymbolizer using the selected channels
        SelectedChannelType[] sct = new SelectedChannelType[srcGc.getNumSampleDimensions()];
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        for (int i = 0; i < 3; i++) {
            sct[i] = sf.createSelectedChannelType(String.valueOf(channelNum[i]), ce);
        }
        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct[RED], sct[GREEN], sct[BLUE]);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }
}
