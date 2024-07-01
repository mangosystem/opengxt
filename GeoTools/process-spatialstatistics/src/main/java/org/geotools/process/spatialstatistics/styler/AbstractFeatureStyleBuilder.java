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
import java.util.logging.Logger;

import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.StyleFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.clsssifier.DataClassify;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.util.logging.Logging;

/**
 * AbstractFeatureStyleBuilder
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractFeatureStyleBuilder {
    protected static final Logger LOGGER = Logging.getLogger(AbstractFeatureStyleBuilder.class);

    protected final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    protected final StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    protected float lineWidth = 1.0f;

    protected float outlineWidth = 0.5f;

    protected float lineOpacity = 1.0f;

    protected float outlineOpacity = 1.0f;

    protected float fillOpacity = 1.0f;

    protected Color outlineColor = new Color(225, 225, 225);

    protected float markerSize = 7.0f;

    public float getMarkerSize() {
        return markerSize;
    }

    public void setMarkerSize(float markerSize) {
        this.markerSize = markerSize;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float outlineWidth) {
        this.outlineWidth = outlineWidth;
    }

    public float getLineOpacity() {
        return lineOpacity;
    }

    public void setLineOpacity(float lineOpacity) {
        this.lineOpacity = lineOpacity;
    }

    public float getOutlineOpacity() {
        return outlineOpacity;
    }

    public void setOutlineOpacity(float outlineOpacity) {
        this.outlineOpacity = outlineOpacity;
    }

    public float getFillOpacity() {
        return fillOpacity;
    }

    public void setFillOpacity(float fillOpacity) {
        this.fillOpacity = fillOpacity;
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color outlineColor) {
        this.outlineColor = outlineColor;
    }

    public RangedClassifier getClassifier(SimpleFeatureCollection inputFeatures,
            String propertyName, String methodName, int numClasses) {
        // ClassificationFunction : JenksNaturalBreaksFunction, EqualIntervalFunction,
        // StandardDeviationFunction, QuantileFunction, UniqueIntervalFunction, CategorizeFunction

        // String functionName = getFunctionName(methodName);
        // Function function = ff.function(functionName, ff.property(propertyName), ff.literal(numClass));
        // return (RangedClassifier) function.evaluate(inputFeatures);

        DataClassify classifier = DataClassify.getDataClassifier(methodName);
        Double[] classBreaks = classifier.classify(inputFeatures, propertyName, numClasses);

        final int size = classBreaks.length - 1;

        Comparable<?>[] localMin = new Comparable[size];
        Comparable<?>[] localMax = new Comparable[size];

        for (int i = 0; i < size; i++) {
            localMin[i] = classBreaks[i];
            localMax[i] = classBreaks[i + 1];
        }

        return new RangedClassifier(localMin, localMax);
    }

    public RangedClassifier getClassifier(SimpleFeatureCollection inputFeatures,
            String propertyName, String normalPropertyName, String methodName, int numClass) {
        // ClassificationFunction : JenksNaturalBreaksFunction, EqualIntervalFunction,
        // StandardDeviationFunction, QuantileFunction, UniqueIntervalFunction, CategorizeFunction

        String functionName = getFunctionName(methodName);
        Divide divide = ff.divide(ff.property(propertyName), ff.property(normalPropertyName));
        Function function = ff.function(functionName, divide, ff.literal(numClass));

        return (RangedClassifier) function.evaluate(inputFeatures);
    }

    private String getFunctionName(String methodName) {
        if (StringHelper.isNullOrEmpty(methodName)) {
            methodName = "Jenks";
        }

        methodName = methodName.toUpperCase();

        String functionName = "Jenks";
        if (methodName.startsWith("NA") || methodName.startsWith("JENK")) {
            functionName = "Jenks";
        } else if (methodName.startsWith("QU")) {
            functionName = "Quantile";
        } else if (methodName.startsWith("EQ")) {
            functionName = "EqualInterval";
        } else if (methodName.startsWith("ST")) {
            functionName = "StandardDeviation";
        } else if (methodName.startsWith("UN")) {
            functionName = "UniqueInterval";
        } else {
            functionName = "Jenks"; // default
        }

        return functionName;
    }

    public RangedClassifier getClassifier(GridCoverage2D coverage, int bandIndex, String methodName,
            int numClasses) {
        DataClassify classifier = DataClassify.getDataClassifier(methodName);
        Double[] classBreaks = classifier.classify(coverage, bandIndex, numClasses);

        final int size = classBreaks.length - 1;

        Comparable<?>[] localMin = new Comparable[size];
        Comparable<?>[] localMax = new Comparable[size];

        for (int i = 0; i < size; i++) {
            localMin[i] = classBreaks[i];
            localMax[i] = classBreaks[i + 1];
        }

        return new RangedClassifier(localMin, localMax);
    }

    public double[] getClassBreaks(RangedClassifier classifier) {
        double[] classBreaks = new double[classifier.getSize() + 1];

        for (int slot = 0; slot < classifier.getSize(); slot++) {
            classBreaks[slot] = (Double) classifier.getMin(slot);
        }

        classBreaks[classifier.getSize()] = (Double) classifier.getMax(classifier.getSize() - 1);

        return classBreaks;
    }
}
