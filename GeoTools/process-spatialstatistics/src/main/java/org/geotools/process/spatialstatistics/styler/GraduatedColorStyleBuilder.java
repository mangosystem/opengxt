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
package org.geotools.process.spatialstatistics.styler;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.transformation.CoverageToPointFeatureCollection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * GraduatedColorStyleBuilder
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GraduatedColorStyleBuilder extends AbstractFeatureStyleBuilder {
    protected static final Logger LOGGER = Logging.getLogger(GraduatedColorStyleBuilder.class);

    String normalProperty;

    ColorBrewer brewer = ColorBrewer.instance();

    public void setNormalProperty(String normalProperty) {
        this.normalProperty = normalProperty;
    }

    public String getNormalProperty() {
        return normalProperty;
    }

    public Style createStyle(GridCoverage2D coverage, String methodName, int numClasses,
            String brewerPaletteName) {
        return createStyle(coverage, methodName, numClasses, brewerPaletteName, false, 1.0d);
    }

    public Style createStyle(GridCoverage2D coverage, String methodName, int numClasses,
            String brewerPaletteName, boolean reverse) {
        return createStyle(coverage, methodName, numClasses, brewerPaletteName, reverse, 1.0d);
    }

    private int checkNumClasses(int numClasses) {
        numClasses = numClasses < 3 ? 3 : numClasses;
        if (numClasses > 12) {
            numClasses = numClasses > 12 ? 12 : numClasses;
            LOGGER.log(Level.WARNING, "maximum numClasses cannot exceed 12!");
        }
        return numClasses;
    }

    public Style createStyle(GridCoverage2D coverage, String methodName, int numClasses,
            String brewerPaletteName, boolean reverse, double opacity) {
        numClasses = checkNumClasses(numClasses);

        SimpleFeatureCollection inputFeatures = new CoverageToPointFeatureCollection(coverage);

        RangedClassifier classifier = getClassifier(inputFeatures, "Value", methodName, numClasses); //$NON-NLS-1$
        double[] breaks = getClassBreaks(classifier);

        BrewerPalette brewerPalette = brewer.getPalette(brewerPaletteName);
        Color[] colors = brewerPalette.getColors(breaks.length - 1);
        if (reverse) {
            Collections.reverse(Arrays.asList(colors));
        }

        StyleBuilder builder = new StyleBuilder();

        // Set nodata
        double noData = RasterHelper.getNoDataValue(coverage);
        ColorMapEntry nodataEntry = sf.createColorMapEntry();
        nodataEntry.setQuantity(ff.literal(noData));
        nodataEntry.setColor(builder.colorExpression(colors[0]));
        nodataEntry.setOpacity(ff.literal(0.0f));
        nodataEntry.setLabel("No Data"); //$NON-NLS-1$

        ColorMap colorMap = sf.createColorMap();
        colorMap.setType(ColorMap.TYPE_RAMP);

        if (noData < breaks[0]) {
            colorMap.addColorMapEntry(nodataEntry);
        }

        for (int i = 0; i < colors.length; i++) {
            colorMap.addColorMapEntry(createColorMapEntry(builder, breaks[i], colors[i]));
        }

        if (noData > breaks[breaks.length - 1]) {
            colorMap.addColorMapEntry(nodataEntry);
        }

        return builder.createStyle(builder.createRasterSymbolizer(colorMap, opacity));
    }

    private ColorMapEntry createColorMapEntry(StyleBuilder sb, double quantity, Color color) {
        ColorMapEntry entry = sf.createColorMapEntry();
        entry.setQuantity(sb.literalExpression(quantity));
        entry.setColor(sb.colorExpression(color));
        entry.setOpacity(sb.literalExpression(color.getAlpha() / 255.0));
        return entry;
    }

    // Diverging: PuOr, BrBG, PRGn, PiYG, RdBu, RdGy, RdYlBu, Spectral, RdYlGn
    // Qualitative: Set1, Pastel1, Set2, Pastel2, Dark2, Set3, Paired, Accents,
    // Sequential: YlGn, YlGnBu, GnBu, BuGn, PuBuGn, PuBu, BuPu, RdPu, PuRd, OrRd, YlOrRd, YlOrBr,
    // Purples, Blues, Greens, Oranges, Reds, Grays,
    public Style createStyle(SimpleFeatureCollection inputFeatures, String propertyName,
            String methodName, int numClasses, String brewerPaletteName, boolean reverse) {
        numClasses = checkNumClasses(numClasses);

        // get classifier
        RangedClassifier classifier = null;
        if (normalProperty == null || normalProperty.isEmpty()) {
            classifier = getClassifier(inputFeatures, propertyName, methodName, numClasses);
        } else {
            classifier = getClassifier(inputFeatures, propertyName, normalProperty, methodName,
                    numClasses);
        }

        double[] classBreaks = getClassBreaks(classifier);

        if (brewerPaletteName == null || brewerPaletteName.isEmpty()) {
            brewerPaletteName = "OrRd"; // default
        }

        BrewerPalette brewerPalette = brewer.getPalette(brewerPaletteName);

        Color[] colors = brewerPalette.getColors(classBreaks.length - 1);
        if (reverse) {
            Collections.reverse(Arrays.asList(colors));
        }

        return createStyle(inputFeatures.getSchema(), propertyName, classBreaks, colors);
    }

    public Style createStyle(SimpleFeatureCollection inputFeatures, String propertyName,
            String methodName, int numClasses, String brewerPaletteName) {
        return createStyle(inputFeatures, propertyName, methodName, numClasses, brewerPaletteName,
                false);
    }

    public Style createStyle(SimpleFeatureType inputFeatureType, String propertyName,
            double[] classBreaks, Color[] colors) {
        if (classBreaks.length - 1 != colors.length) {
            LOGGER.log(Level.FINE, "classBreaks's length dose not equal colors's length");
            return null;
        }

        GeometryDescriptor geomDesc = inputFeatureType.getGeometryDescriptor();
        String geometryPropertyName = geomDesc.getLocalName();
        SimpleShapeType shapeType = FeatureTypes
                .getSimpleShapeType(geomDesc.getType().getBinding());

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        PropertyName property = ff.property(propertyName);
        for (int k = 0, length = classBreaks.length - 2; k <= length; k++) {
            Expression color = ff.literal(colors[k]);
            Expression lowerClass = ff.literal(classBreaks[k]);
            Expression upperClass = ff.literal(classBreaks[k + 1]);

            Symbolizer symbolizer = null;
            switch (shapeType) {
            case POINT:
                Mark mark = sf.getCircleMark();
                Stroke markStroke = sf.createStroke(ff.literal(Color.WHITE),
                        ff.literal(outlineWidth), ff.literal(outlineOpacity));
                mark.setStroke(markStroke);
                mark.setFill(sf.createFill(color, ff.literal(fillOpacity)));

                Graphic graphic = sf.createDefaultGraphic();
                graphic.graphicalSymbols().clear();
                graphic.graphicalSymbols().add(mark);
                graphic.setSize(ff.literal(markerSize));

                symbolizer = sf.createPointSymbolizer(graphic, geometryPropertyName);
                break;
            case LINESTRING:
                Stroke lineStroke = sf.createStroke(color, ff.literal(lineWidth),
                        ff.literal(lineOpacity));

                symbolizer = sf.createLineSymbolizer(lineStroke, geometryPropertyName);
                break;
            case POLYGON:
                Stroke outlineStroke = sf.createStroke(ff.literal(outlineColor),
                        ff.literal(outlineWidth), ff.literal(outlineOpacity));

                Fill fill = sf.createFill(color, ff.literal(fillOpacity));
                symbolizer = sf.createPolygonSymbolizer(outlineStroke, fill, geometryPropertyName);
                break;
            }

            Filter lower = ff.greaterOrEqual(property, lowerClass);
            Filter upper = k == length ? ff.lessOrEqual(property, upperClass) : ff.less(property,
                    upperClass);

            Rule rule = sf.createRule();
            rule.setName(classBreaks[k] + " - " + classBreaks[k + 1]);
            rule.setFilter(ff.and(lower, upper));
            rule.symbolizers().add(symbolizer);

            fts.rules().add(rule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
    }

}
