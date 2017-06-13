/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.styler;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.styling.Style;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

/**
 * Output Styler
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class OutputStyler {

    // OutputStyler styler = OutputStylerFactory.getStyler(source, "LISA");
    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    protected Object source;

    protected float opacity = 1.0f;

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public OutputStyler(Object source) {
        this.source = source;
    }

    public abstract Style getStyle();

    protected RangedClassifier getClassifier(SimpleFeatureCollection inputFeatures,
            String propertyName, String methodName, int numClass) {
        String functionName = getFunctionName(methodName);
        Function function = ff.function(functionName, ff.property(propertyName),
                ff.literal(numClass));
        return (RangedClassifier) function.evaluate(inputFeatures);
    }

    protected String getFunctionName(String methodName) {
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

    protected double[] getClassBreaks(RangedClassifier classifier) {
        double[] classBreaks = new double[classifier.getSize() + 1];

        for (int slot = 0; slot < classifier.getSize(); slot++) {
            classBreaks[slot] = (Double) classifier.getMin(slot);
        }

        classBreaks[classifier.getSize()] = (Double) classifier.getMax(classifier.getSize() - 1);

        return classBreaks;
    }
}
