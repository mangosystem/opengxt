package org.locationtech.udig.processingtoolbox.styler;

import org.geotools.styling.Style;

public class GraduatedColorGridCoverageStyler extends OutputStyler {

    public GraduatedColorGridCoverageStyler(Object source, String functionName, int numClasses,
            String brewerPaletteName) {
        super(source);
    }

    @Override
    public Style getStyle() {
        return null;
    }

}
