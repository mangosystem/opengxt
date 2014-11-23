/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * GridCoverage Layer Data control
 * 
 * @author MapPlus
 * 
 */
public class GridCoverageDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(GridCoverageDataWidget.class);

    private IMap map;

    public GridCoverageDataWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        GridLayout layout = new GridLayout(1, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        final Combo cboGcLayer = new Combo(composite, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
        cboGcLayer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        fillLayers(map, cboGcLayer);
        cboGcLayer.setData(param.key);

        cboGcLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                GridCoverage2D sfc = getGridCoverage(map, cboGcLayer.getText());
                processParams.put(param.key, sfc);
            }
        });

        composite.pack();
    }

    private void fillLayers(IMap map, Combo combo) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(GridCoverageReader.class)) {
                combo.add(layer.getName());
            } else if (layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                combo.add(layer.getName());
            }
        }
    }

    private GridCoverage2D getGridCoverage(IMap map, String layerName) {
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName().equals(layerName)) {
                try {
                    if (layer.hasResource(GridCoverage2D.class)) {
                        return layer.getResource(GridCoverage2D.class, new NullProgressMonitor());
                    } else if (layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                        GridCoverageReader reader = layer.getResource(GridCoverageReader.class,
                                null);
                        return (GridCoverage2D) reader.read(null);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
        }
        return null;
    }
}
