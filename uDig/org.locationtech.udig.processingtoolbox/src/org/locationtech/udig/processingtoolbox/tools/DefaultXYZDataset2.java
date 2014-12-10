/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.util.List;

import org.jfree.data.xy.DefaultXYZDataset;

/**
 * DefaultXYZDataset extends JFreeChart
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DefaultXYZDataset2 extends DefaultXYZDataset {

    private static final long serialVersionUID = 3557146047888892317L;

    /**
     * Storage for the series keys. This list must be kept in sync with the seriesList.
     */
    private List<String[]> featureIDS;

    public DefaultXYZDataset2() {
        super();
        this.featureIDS = new java.util.ArrayList<String[]>();
    }

    public String getFeatrureID(int series, int item) {
        return this.featureIDS.get(series)[item];
    }

    @SuppressWarnings("rawtypes")
    public void addFeatrureIDS(Comparable seriesKey, String[] featureIDS) {
        if (this.featureIDS.size() > 0) {
            this.featureIDS.clear();
        }
        this.featureIDS.add(featureIDS);
    }
}
