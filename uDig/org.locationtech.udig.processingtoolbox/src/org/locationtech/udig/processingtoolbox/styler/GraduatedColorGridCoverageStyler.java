package org.locationtech.udig.processingtoolbox.styler;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.transformation.CoverageToPointFeatureCollection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;

public class GraduatedColorGridCoverageStyler extends OutputStyler {

    private String methodName;

    private int numClasses;

    private String brewerPaletteName;

    private boolean reverse;

    public GraduatedColorGridCoverageStyler(Object source, String methodName, int numClasses,
            String brewerPaletteName, boolean reverse) {
        super(source);

        this.methodName = methodName;
        this.numClasses = numClasses;
        if (brewerPaletteName == null || brewerPaletteName.isEmpty()) {
            brewerPaletteName = "OrRd"; // default
        }
        this.brewerPaletteName = brewerPaletteName;
        this.reverse = reverse;
    }

    @Override
    public Style getStyle() {
        ColorBrewer brewer = ColorBrewer.instance();

        GridCoverage2D coverage = (GridCoverage2D) source;
        SimpleFeatureCollection inputFeatures = new CoverageToPointFeatureCollection(coverage);

        RangedClassifier classifier = getClassifier(inputFeatures, "Value", methodName, numClasses);
        double[] classBreaks = getClassBreaks(classifier);

        BrewerPalette brewerPalette = brewer.getPalette(brewerPaletteName);
        Color[] brewerColors = brewerPalette.getColors(classBreaks.length - 1);
        if (reverse) {
            Collections.reverse(Arrays.asList(brewerColors));
        }

        // set nodata
        double noData = RasterHelper.getNoDataValue(coverage);

        int size = brewerColors.length;
        String[] labels = new String[size];
        double[] quantities = new double[size];
        java.awt.Color[] colors = new Color[size];

        int start = noData <= classBreaks[0] ? 1 : 0;

        for (int index = 0; index < size; index++) {
            labels[index] = String.format("%s", quantities[index]);
            quantities[index] = classBreaks[index];
            colors[index] = brewerColors[index];
        }

        StyleBuilder sb = new StyleBuilder();
        ColorMap colorMap = sb.createColorMap(labels, quantities, colors, ColorMap.TYPE_RAMP);
        return sb.createStyle(sb.createRasterSymbolizer(colorMap, opacity));
    }

}
