package org.locationtech.udig.processingtoolbox.tools;

import org.jfree.data.xy.XYDataItem;
import org.opengis.feature.simple.SimpleFeature;

public class XYDataItem2 extends XYDataItem {
    private static final long serialVersionUID = 2567264473390904488L;

    private SimpleFeature feature;

    public XYDataItem2(SimpleFeature feature, double x, double y) {
        super(x, y);
        this.feature = feature;
    }

    public SimpleFeature getFeature() {
        return this.feature;
    }
}
