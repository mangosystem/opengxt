/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.geotools.factory.CommonFactoryFinder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.locationtech.udig.project.ILayer;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

/**
 * ChartComposite extends JFreeChart
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
class ChartComposite2 extends ChartComposite {

    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private org.locationtech.udig.project.internal.Map map;

    private ILayer layer;

    public org.locationtech.udig.project.internal.Map getMap() {
        return map;
    }

    public void setMap(org.locationtech.udig.project.internal.Map map) {
        this.map = map;
    }

    public ILayer getLayer() {
        return layer;
    }

    public void setLayer(ILayer layer) {
        this.layer = layer;
    }

    public ChartComposite2(Composite comp, int style) {
        super(comp, style);
    }

    public ChartComposite2(Composite comp, int style, JFreeChart chart) {
        super(comp, style, chart);
    }

    public ChartComposite2(Composite comp, int style, JFreeChart chart, boolean useBuffer) {
        super(comp, style, chart, useBuffer);
    }

    public ChartComposite2(Composite comp, int style, JFreeChart chart, boolean properties,
            boolean save, boolean print, boolean zoom, boolean tooltips) {
        super(comp, style, chart, properties, save, print, zoom, tooltips);
    }

    public ChartComposite2(Composite comp, int style, JFreeChart jfreechart, int width, int height,
            int minimumDrawW, int minimumDrawH, int maximumDrawW, int maximumDrawH,
            boolean usingBuffer, boolean properties, boolean save, boolean print, boolean zoom,
            boolean tooltips) {
        super(comp, style, jfreechart, width, height, minimumDrawW, minimumDrawH, maximumDrawW,
                maximumDrawH, usingBuffer, properties, save, print, zoom, tooltips);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void zoom(Rectangle selection) {
        if (map == null || layer == null) {
            return;
        }
        Set<FeatureId> selected = new HashSet<FeatureId>();
        try {
            XYSeriesCollection ds = (XYSeriesCollection) getChart().getXYPlot().getDataset(2);
            XYSeries selectionSeries = ds.getSeries(0);
            selectionSeries.clear();

            EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
            Iterator iter = entities.iterator();
            while (iter.hasNext()) {
                ChartEntity entity = (ChartEntity) iter.next();
                if (entity instanceof XYItemEntity) {
                    XYItemEntity item = (XYItemEntity) entity;
                    if (item.getSeriesIndex() != 0) {
                        continue;
                    }

                    java.awt.Rectangle bound = item.getArea().getBounds();
                    if (selection.intersects(bound.x, bound.y, bound.width, bound.height)) {
                        XYSeriesCollection dataSet = (XYSeriesCollection) item.getDataset();
                        XYSeries xySeries = dataSet.getSeries(item.getSeriesIndex());
                        XYDataItem xyDataItem = xySeries.getDataItem(item.getItem());
                        if (xyDataItem instanceof XYDataItem2) {
                            XYDataItem2 dataItem = (XYDataItem2) xyDataItem;
                            selectionSeries.add(dataItem);
                            selected.add(ff.featureId(dataItem.getFeature().getID()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // skip
        } finally {
            if (selected.size() > 0) {
                map.select(ff.id(selected), layer);
            } else {
                map.select(Filter.EXCLUDE, layer);
            }
            this.forceRedraw();
        }
    }

    @Override
    public void restoreAutoBounds() {
        return;
    }

}
